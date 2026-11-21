# NEXT_TASK_REDLAB — 2026-11-21 — كود نملكه ونطوّره

## السياق (لا تعيد اختراعه)
- المستودع: `/storage/emulated/0/red/repo` · remote `PanditJi-9/Xx` · branch `main`.
- العقد: `AGENTS.md` + `docs/ai/ENGINEERING-CONTRACT.md` + `docs/ai/VALIDATION.md` + `docs/ai/REVIEW.md`.
- لا بناء تلقائيًا (§0.1): بوابات `kt_balance`/`code_health`/`i18n_coverage`/`design_tokens`/`resource_compile` تكفي لتعديلات الواجهة؛ `./gradlew :app:compileReleaseKotlin` فقط عند حاجة حقيقية (§0.1-3).
- لا أسرار في الملفات · push مسموح على `PanditJi-9/Xx` · لغات جديدة: EN + AR فقط (§0.2).

## ما أنجز (مقيس)
- `redlab/apps/gamespace/src/ported/java/` = **278 ملفًا (3.2 م.ب)** مملوكة (git-tracked)، ثُبّتت بعد أن قبلها `javac`.
- `redlab/apps/gamespace/PORTED.md` + `SEALED.json` يوثّقان (corpus 6280 · مكتبات 5217 · مرشَّحة 404 · مملوكة 278 · مرفوضة 126).
- CI `redlab-port.yml` = **success** (آخر تشغيل `44865094295`)، `assembleDebug` BUILD SUCCESSFUL.
- `redlab/apps/gameassist/` = shell يبني APK (main + strings)؛ materials + ported code مؤجَّلان.
- `tools/port_materials.py` يتجاهل layouts referencing `android:id/search_src_text` (private framework id) — narrowed to the framework reference only.
- `docs/MASTER_PLAN.md §10` و`docs/REBUILD_LEDGER.md §3.2/§3.3` محدّثان.

## الخطوة التالية (الأولوية)
1. **الـ126 وحدة المرفوضة**: اجمع أسباب الرفض (من `/tmp/javac.log` في CI) into `docs/port/FRONTIER.md`. største Gruppe: `library not linked` (androidx.preference، gson، okhttp…). أضفها كـ`compileOnly`/`implementation` **بدون** تعارض موارد (androidx.preference فشل أصلًا بـ`Can not add resource ... to table`). الهدف: ترجمة ≥ 50 weitere Einheiten.
2. **GameAssist**: حلّ اعتماد appcompat (abc_search_view/edit_query/overflow_button) — إما إضافة appcompat dependency مع استبعاد مواردها المكرّرة، أو تشذيب resources.
3. لا تنسَ: كل نص جديد في `values/` + `values-ar/` فقط (§0.2)، التثبيت add-only (لا حذف).

## أوامر مفيدة
```sh
gh workflow run redlab-port.yml -R PanditJi-9/Xx --ref main
gh run view <id> -R PanditJi-9/Xx --json status,conclusion
python3 tools/port_slice.py --sources work/port/GameSpace/sources --package cn.nubia.gamelauncher --app GameSpace --json work/port/GameSpace/slice.json
python3 tools/seal_ported.py --self-test
python3 tools/port_materials.py --self-test
```

## لا تفعل
- لا تُعد تثبيت (seal) أي شيء حُذف (add-only).
- لا تقليد شكل/أيقونات من تطبيقات مرجعية (§0.4).
- لا تدّعِ «١٠٠٪ تغطية» لغات (فقط EN/AR).
- لا تبني شاشة معدلة لمجرد الاطمئنان (§0.1).
