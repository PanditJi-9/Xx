/*
 * Copyright (C) 2025-2026 Ryanistr
 *
 * Derived from Rianixia-ThermalCore (https://github.com/ryanistr/Rianixia-ThermalCore),
 * modified for MaxManager (modifications: Copyright (C) 2026 Nader Magdy).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//! التنبؤ باتجاه الحرارة — **قراءة لا قرار** (تكملة `PERF-AUDIT-01-CLOSE`).
//!
//! ### ما كان هذا الملفّ، وما صار
//!
//! كان نسخةً من نموذج تعلّم قديم يقرأ **سبعة حقول لا وجود لها** في `learning::ThermalEvent`
//! (`temp_delta` · `core_load_max` · `is_charging` · `current_now_abs` · `hour_of_day` ·
//! `avg_gradient` …)، و**خارج شجرة الوحدات** أصلًا — فلا يُصرَّف ولا يُقاس، ويبقى يتعفّن بلا
//! خطأ (وهو ما كانت تسمّيه بوابة `dead_modules` بلا أن تسقطه: `ADR-18` يمنع الحذف لأسباب
//! جمالية، وإصلاحه قرار سلوك).
//!
//! واليوم صار قرارًا معلَنًا: **يُعاد بناؤه على ما نقيسه فعلًا** — الحرارة والعيّنة الزمنية —
//! ويُوصَل بالمراقب ليُسجَّل في سطر التشخيص وحده، **بلا أي أثر على قرار التبريد**. والسبب:
//! نموذجٌ يُغيّر شدة التبريد قبل بلوغ العتبة يحتاج **معايرة على جهاز**؛ أما تنبؤٌ يُكتب في
//! السجلّ فيكشف الاتجاه مبكرًا ولا يخاطر بشيء.
//!
//! ### الرياضيات: انحدار خطّي بالمربعات الصغرى على الزمن الحقيقي
//!
//! النموذج القديم كان جمعًا موزونًا بثمانية أوزان ثابتة تُحدَّث بمعدّل تعلّم — وبلا معايرة على
//! جهاز ينتج أرقامًا بلا معنى، ويستطيع أن ينحرف بلا حدّ. وصار هنا **خطًّا واحدًا** يُقدَّر من
//! العيّنات نفسها:
//!
//! ```text
//! slope = Σ (tᵢ − t̄)(vᵢ − v̄) / Σ (tᵢ − t̄)²        [عُشر درجة / ثانية]
//! v(t + h) = v_last + slope × h
//! ```
//!
//! وثلاثة حرّاس تجعل الحكم صادقًا:
//!
//! 1. **أقلّ من [PREDICTION_MIN_SAMPLES] عيّنات ⇒ لا تنبؤ** (`None`) لا صفرٌ مخترع (`ADR-07`).
//! 2. **تباين زمني صفري ⇒ لا تنبؤ**: عيّنتان في اللحظة نفسها لا تُنتجان ميلًا، والقسمة على صفر
//!    كانت ستُنتج `NaN` يُطبَع كرقم.
//! 3. **حدّا حرارة [PREDICTION_MIN_TEMP_DECI_C] و[PREDICTION_MAX_TEMP_DECI_C]**: ميل شاذّ
//!    (عيّنة واحدة قافزة) لا يُنتج تنبؤًا بمئات الدرجات يُقرأ عطبًا.
//!
//! **والحدّ المعلن:** هذا استقراء خطّي لا نموذج حراري؛ لا يعرف الشحن ولا حمل المعالج ولا
//! منحنى التبريد. فائدته أن يُظهر **الاتجاه أثناء الصعود** في سطر التشخيص، لا أن يُتَّخذ عليه
//! قرار — ومن أراد القرار فليُعاير على جهاز ويُسجّل `ADR` جديد.

use std::collections::VecDeque;

use super::constants::{
    PREDICTION_HORIZON_SECS, PREDICTION_MAX_TEMP_DECI_C, PREDICTION_MIN_SAMPLES,
    PREDICTION_MIN_TEMP_DECI_C, PREDICTION_WINDOW_SAMPLES,
};

/// عيّنة حرارة واحدة: اللحظة (ثوانٍ على الساعة الرتيبة) والحرارة (عُشر درجة مئوية).
///
/// والعيّنة **لا تحمل تفسيرًا** — لا حمل ولا شحن — لأن ما لا تقيسه النواة لا يُخترع هنا.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct TemperatureSample {
    pub at_secs: u64,
    pub deci_c: i32,
}

/// تنبؤ عند أفق زمني محدّد — والوحدة عُشر درجة مئوية، كبقية `thermalcore`.
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Forecast {
    /// الحرارة المتوقَّعة بعد `horizon_secs` ثانية، محبوسة بين الحدّين المعلَنين.
    pub predicted_deci_c: i32,
    /// الميل المقيس — عُشر درجة في الدقيقة (سالب عند التبريد).
    pub slope_per_minute_deci_c: f32,
    /// الأفق الذي حُسب عليه التنبؤ (ثانية).
    pub horizon_secs: u64,
}

/// نافذة انحدار متحرّكة — تُغذّى عيّنةً عيّنة، ولا تحتفظ بتاريخ أطول من [PREDICTION_WINDOW_SAMPLES].
#[derive(Debug, Default)]
pub struct TrendPredictor {
    samples: VecDeque<TemperatureSample>,
}

impl TrendPredictor {
    pub fn new() -> Self {
        Self {
            samples: VecDeque::with_capacity(PREDICTION_WINDOW_SAMPLES),
        }
    }

    /// عدد العيّنات المحفوظة الآن (للتشخيص والاختبار).
    pub fn samples(&self) -> usize {
        self.samples.len()
    }

    /// يضيف عيّنة ويُبقي النافذة على أحدثها.
    ///
    /// **واللحظة المكرّرة تستبدل لا تُضاف:** المراقب يقرأ بالثانية نفسها أحيانًا (حدث حرارة
    /// فوريّ يعقب تقييمًا احتياطيًّا في الثانية ذاتها)؛ وإضافة الثانية نفسها مرّتين تُفسد
    /// الانحدار بميل لا معنى له.
    pub fn push(&mut self, at_secs: u64, deci_c: i32) {
        if self.samples.back().is_some_and(|last| last.at_secs == at_secs) {
            self.samples.pop_back();
        }
        while self.samples.len() >= PREDICTION_WINDOW_SAMPLES {
            self.samples.pop_front();
        }
        self.samples.push_back(TemperatureSample { at_secs, deci_c });
    }

    /// ميل الانحدار (عُشر درجة/ثانية)، أو `None` إن لم تكفِ العيّنات أو كان التباين الزمني صفرًا.
    pub fn slope_per_second(&self) -> Option<f32> {
        if self.samples.len() < PREDICTION_MIN_SAMPLES {
            return None;
        }
        let count = self.samples.len() as f64;
        let mean_time = self.samples.iter().map(|s| s.at_secs as f64).sum::<f64>() / count;
        let mean_value = self.samples.iter().map(|s| s.deci_c as f64).sum::<f64>() / count;

        let mut covariance = 0.0;
        let mut variance = 0.0;
        for sample in &self.samples {
            let dt = sample.at_secs as f64 - mean_time;
            covariance += dt * (sample.deci_c as f64 - mean_value);
            variance += dt * dt;
        }
        if variance <= f64::EPSILON {
            return None;
        }
        Some((covariance / variance) as f32)
    }

    /// تنبؤ عند أفق صريح — يُستعمل في الاختبار وفي أي مستهلك يريد أفقًا آخر.
    pub fn forecast(&self, horizon_secs: u64) -> Option<Forecast> {
        let slope = self.slope_per_second()?;
        let last = self.samples.back()?;
        let predicted = last.deci_c as f64 + slope as f64 * horizon_secs as f64;
        let clamped = predicted.round().clamp(
            PREDICTION_MIN_TEMP_DECI_C as f64,
            PREDICTION_MAX_TEMP_DECI_C as f64,
        ) as i32;
        Some(Forecast {
            predicted_deci_c: clamped,
            slope_per_minute_deci_c: slope * 60.0,
            horizon_secs,
        })
    }

    /// تنبؤ الأفق الافتراضي ([PREDICTION_HORIZON_SECS]) — ما يقرؤه سطر التشخيص.
    pub fn forecast_default(&self) -> Option<Forecast> {
        self.forecast(PREDICTION_HORIZON_SECS)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// يغذّي النموذج عيّنة كل ٥ ثوانٍ — نفس إيقاع التقييم الاحتياطيّ في `monitor`.
    fn feed(predictor: &mut TrendPredictor, values: &[i32]) {
        for (index, value) in values.iter().enumerate() {
            predictor.push(index as u64 * 5, *value);
        }
    }

    #[test]
    fn fewer_than_the_minimum_samples_has_no_forecast() {
        let mut predictor = TrendPredictor::new();
        feed(
            &mut predictor,
            &vec![300; PREDICTION_MIN_SAMPLES - 1],
        );
        assert_eq!(predictor.samples(), PREDICTION_MIN_SAMPLES - 1);
        assert_eq!(predictor.slope_per_second(), None);
        assert_eq!(predictor.forecast_default(), None);
    }

    #[test]
    fn a_flat_line_predicts_the_same_value() {
        let mut predictor = TrendPredictor::new();
        feed(&mut predictor, &[300, 300, 300, 300, 300, 300]);
        let forecast = predictor.forecast(30).expect("six samples is enough");
        assert_eq!(forecast.predicted_deci_c, 300);
        assert!(forecast.slope_per_minute_deci_c.abs() < f32::EPSILON);
    }

    #[test]
    fn a_rising_line_is_extrapolated_from_the_last_sample() {
        let mut predictor = TrendPredictor::new();
        // ‏+5 عُشر كل ٥ ثوانٍ = ١ عُشر/ثانية.
        feed(&mut predictor, &[300, 305, 310, 315, 320]);
        let forecast = predictor.forecast(30).expect("five samples is enough");
        assert_eq!(forecast.predicted_deci_c, 350);
        assert!((forecast.slope_per_minute_deci_c - 60.0).abs() < 0.01);
    }

    #[test]
    fn a_falling_line_is_extrapolated_downwards() {
        let mut predictor = TrendPredictor::new();
        feed(&mut predictor, &[350, 345, 340, 335, 330]);
        let forecast = predictor.forecast(20).expect("five samples is enough");
        assert_eq!(forecast.predicted_deci_c, 310);
        assert!((forecast.slope_per_minute_deci_c + 60.0).abs() < 0.01);
    }

    #[test]
    fn repeated_timestamps_cannot_divide_by_zero() {
        let mut predictor = TrendPredictor::new();
        for _ in 0..PREDICTION_MIN_SAMPLES {
            predictor.push(7, 400);
        }
        assert_eq!(predictor.samples(), 1, "same instant replaces, does not accumulate");
        assert_eq!(predictor.forecast(45), None);
    }

    #[test]
    fn an_absurd_slope_is_clamped_to_the_declared_ceiling() {
        let mut predictor = TrendPredictor::new();
        // ‏+200 عُشر كل ٥ ثوانٍ = ٤٠ عُشر/ثانية ⇒ ١٨٠٠ بعد ٤٥ ثانية بلا حبس.
        feed(&mut predictor, &[0, 200, 400, 600, 800]);
        let forecast = predictor.forecast_default().expect("five samples is enough");
        assert_eq!(forecast.predicted_deci_c, PREDICTION_MAX_TEMP_DECI_C);
    }

    #[test]
    fn the_window_keeps_only_the_newest_samples() {
        let mut predictor = TrendPredictor::new();
        feed(&mut predictor, &vec![300; PREDICTION_WINDOW_SAMPLES + 8]);
        assert_eq!(predictor.samples(), PREDICTION_WINDOW_SAMPLES);
    }
}
