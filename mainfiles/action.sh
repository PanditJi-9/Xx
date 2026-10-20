#!/system/bin/sh

#
# Copyright (C) 2026-2027 Zexshia
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

readonly MODDIR="${0%/*}"
readonly BIN_SVC="$MODDIR/system/bin/sys.maxmanager-service"

readonly APK_COMP="$MODDIR/app/MaxManager.apk"

# Check if app is installed
_app_installed() {
	pm path nd.max >/dev/null 2>&1
}

# Check if service binary exists
_service_exists() {
	[ -f "$BIN_SVC" ]
}

# The APK is a user app that service.sh installs from app/MaxManager.apk. This
# action repairs a missing install (below) and launches the app.
clear

if ! _service_exists; then
	echo "[!] Service binary not found at $BIN_SVC" >&2
	exit 1
fi

if ! _app_installed; then
	if pm list packages -u 2>/dev/null | grep -qx 'package:nd.max'; then
		echo "[*] Restoring MaxManager for user 0..."
		cmd package install-existing --user 0 nd.max
	fi
fi

# Missing install: install the module's APK as a user app (service.sh does the same after boot).
if ! _app_installed && [ -f "$APK_COMP" ]; then
	echo "[*] Installing MaxManager from the module..."
	cp "$APK_COMP" /data/local/tmp/MaxManager.apk
	chmod 644 /data/local/tmp/MaxManager.apk
	pm install -r --user 0 /data/local/tmp/MaxManager.apk >/dev/null 2>&1 \
		|| echo "[!] Install failed (root may be required)."
	rm -f /data/local/tmp/MaxManager.apk
fi

if _app_installed; then
	echo "[*] Launching MaxManager..."
	exec "$BIN_SVC" --appactivity
fi

if [ -f "$APK_COMP" ]; then
	echo "[!] MaxManager is in the module but is not installed yet; reboot once so service.sh installs it." >&2
else
	echo "[!] MaxManager APK is missing from the module (app/MaxManager.apk)." >&2
fi
exit 1
