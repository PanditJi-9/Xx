#!/usr/bin/env bash
# ══════════════════════════════════════════════════════════════════════════════
# module_payload.sh — يبني حِمل الوحدة **بلا الـAPK** ليُضمَّن داخل التطبيق
# ══════════════════════════════════════════════════════════════════════════════
#
# **لماذا وُجد:** أمر المالك (٢٠٢٦-١١-١٦): من ثبّت APK وحده — بلا الوحدة — لا يجد
# ميزات الإقلاع، ويريد أن يستطيع تفعيلها **بلا إنترنت**. فالـAPK يحمل الحِمل داخل
# `assets/module_payload.zip`، ويكتبه بنفسه عند منح الجذر.
#
# **وما لا يُحمل:** `app/MaxManager.apk` (٦٤ م.ب من ٦٦) و`META-INF/` (توقيع الـZIP
# لا معنى له داخل APK موقّع) و`README.md`/`customize.sh` (نصّان لمثبّت Magisk لا
# يقرؤهما التطبيق). ⇒ الحِمل ≈ ٢٫٩ م.ب: السكربتات + `libs/` + `system/bin`.
#
# **ومرجع التجميع هو `.github/scripts/compile_zip.sh` نفسه** (الأسطر ٥٩-١٢٢):
# المصادر الأربعة نفسها (`libs/` · `archdaemon/libs/` · `preloadbin/libs/` ·
# `target/<triple>/$RUST_PROFILE/`)، وأسماء الملفّات نفسها. وأي انحراف بين
# السكربتين يُسقط البوابة (`tools/module_payload_check.py`) بدل أن يُشحن حِمل ناقص.
#
# الاستعمال:  bash .github/scripts/module_payload.sh [release|debug] <out.zip>
# ══════════════════════════════════════════════════════════════════════════════
set -euo pipefail

RUST_PROFILE="${1:-release}"
OUT="${2:?الاستعمال: module_payload.sh <release|debug> <out.zip>}"
[ "$RUST_PROFILE" = "release" ] || [ "$RUST_PROFILE" = "debug" ] || {
	echo "❌ profile غير معروف: $RUST_PROFILE" >&2; exit 1; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
PAYLOAD="$WORK/payload"

fail() { echo "❌ $*" >&2; exit 1; }

# ── ١) السكربتات وملفّات الوحدة (بلا APK وبلا META-INF) ─────────────────────
#
# **ومصدر كل ملفّ يُقال صراحةً (عطب مقيس في CI، ٢٠٢٦-١١-١٧):** طُلبت هنا
# `mainfiles/maxmanagerApplist.json` و`mainfiles/LICENSE` و`mainfiles/THIRD_PARTY_NOTICES.md`،
# وليست في الشجرة — لأن `compile_zip.sh` **ينسخها من جذر المستودع إلى `mainfiles/`** قبل
# التغليف. فسقط التشغيل بـ«مفقود أو فارغ» بعد تصليب DEX كامل. والقاعدة الآن مطابقة
# لمصدر الحقيقة: السكربتات من `mainfiles/`، والملفّات الثلاثة من جذر المستودع.
FILES=(
	module.prop post-fs-data.sh service.sh props.sh preferenced-tweaks.sh
	action.sh verify.sh uninstall.sh
)
mkdir -p "$PAYLOAD"
for name in "${FILES[@]}"; do
	[ -s "mainfiles/$name" ] || fail "mainfiles/$name مفقود أو فارغ — الحِمل لا يُبنى ناقصًا"
	cp "mainfiles/$name" "$PAYLOAD/$name"
done

# `maxmanagerApplist.json` **إلزاميّ** (يقرأه التطبيق ليعرف ما يفعّله)، والاثنان الآخران
# اختياريّان كما في `compile_zip.sh` (`2>/dev/null` هناك) — فلا يُسقط غيابُهما بناءً.
[ -s maxmanagerApplist.json ] || fail "maxmanagerApplist.json مفقود أو فارغ في جذر المستودع"
cp maxmanagerApplist.json "$PAYLOAD/maxmanagerApplist.json"
for name in LICENSE THIRD_PARTY_NOTICES.md; do
	[ -s "$name" ] && cp "$name" "$PAYLOAD/$name"
done

# ── ٢) system/ ثم المكتبات: نفس مصادر compile_zip.sh وأسماؤها ──────────────
mkdir -p "$PAYLOAD/system/bin" "$PAYLOAD/libs/arm64-v8a" "$PAYLOAD/libs/armeabi-v7a"
if [ -d "mainfiles/system" ]; then
	cp -r mainfiles/system/. "$PAYLOAD/system/"
fi
rm -f "$PAYLOAD/system/bin/.placeholder"
[ -d "mainfiles/libs" ] && cp -r mainfiles/libs/. "$PAYLOAD/libs/"

copy_binary() {
	local src="$1" dst="$2"
	[ -f "$src" ] || fail "ثنائيّ مفقود: $src"
	cp "$src" "$dst"
	chmod 0755 "$dst"
}

[ -d libs ] && cp -r libs/. "$PAYLOAD/libs/"
[ -d archdaemon/libs ] && cp -r archdaemon/libs/. "$PAYLOAD/libs/"
[ -d preloadbin/libs ] && cp -r preloadbin/libs/. "$PAYLOAD/libs/"

for abi_pair in "aarch64-linux-android:arm64-v8a" "armv7-linux-androideabi:armeabi-v7a"; do
	triple="${abi_pair%%:*}"
	abi="${abi_pair##*:}"
	base="$PAYLOAD/libs/$abi"
	mkdir -p "$base"
	copy_binary "thermalcore/target/$triple/$RUST_PROFILE/rianixia-thermalcore" \
		"$base/sys.maxmanager-rianixiathermalcore"
	copy_binary "binprofiles/target/$triple/$RUST_PROFILE/maxmanager-profilesettings" \
		"$base/sys.maxmanager-profilesettings"
	copy_binary "binutils/target/$triple/$RUST_PROFILE/maxmanager-utilityconf" \
		"$base/sys.maxmanager-utilityconf"
done

DAEMON_NAME="sys.maxmanager-service"
copy_binary "archdaemon/libs/arm64-v8a/$DAEMON_NAME" "$PAYLOAD/system/bin/$DAEMON_NAME"

# ── ٣) صلاحيات الحِمل كما يفرضها المثبّت: سكربتات 0755، وبقية 0644 ──────────
find "$PAYLOAD" -type f \( -name '*.sh' -o -path "$PAYLOAD/system/bin/*" \) -exec chmod 0755 {} +
find "$PAYLOAD" -type f ! -name '*.sh' ! -path "$PAYLOAD/system/bin/*" -exec chmod 0644 {} +

# ── ٤) التغليف (بلا ميتاداتا وقت، فيبقى الناتج قابلًا للمقارنة) ────────────
mkdir -p "$(dirname "$OUT")"
( cd "$PAYLOAD" && zip -q -r -X "$OUT" . -x '.*' )
[ -s "$OUT" ] || fail "لم يُكتب $OUT"

count=$(unzip -Z1 "$OUT" | wc -l)
size=$(wc -c < "$OUT")
echo "✓ حِمل الوحدة: $count مدخلًا · $size بايت · $OUT"
