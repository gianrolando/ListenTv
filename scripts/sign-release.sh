#!/bin/sh
set -eu
if [ "$#" -ne 6 ]; then
  echo "Usage: $0 UNSIGNED_APK APKSIGNER_JAR PKCS12_KEYSTORE PASSWORD_FILE OUTPUT_APK JAVA_BINARY" >&2
  exit 2
fi
"$6" -jar "$2" sign --ks "$3" --ks-type PKCS12 --ks-key-alias listentv --ks-pass "file:$4" --key-pass "file:$4" --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --out "$5" "$1"
"$6" -jar "$2" verify --verbose --print-certs "$5"
