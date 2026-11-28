#!/usr/bin/env bash
# Sets the version of all modules, as described in Release-HOWTO.md:
#
#   ./set-version.sh 0.32.0            # for a release
#   ./set-version.sh 0.32.1-SNAPSHOT   # back to development
#
# It runs tycho-versions-plugin for the OSGi modules (pom.xml, MANIFEST.MF,
# feature.xml, category.xml), versions:set for the plain Maven modules (repl,
# standalone.tests, webdemo) and sets the core dependency of the webdemo. Then
# it checks that the old version is gone and that the new one is in as many
# places as the old one was. It does not commit.
set -euo pipefail

cd "$(dirname "$0")"

if [[ $# -ne 1 || ! "$1" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-SNAPSHOT)?$ ]]; then
	echo "Usage: $0 X.Y.Z[-SNAPSHOT]" >&2
	exit 2
fi
new="$1"

# The version of the root pom (it has no parent, so its first <version> is its own)
old=$(sed -n 's:^[[:space:]]*<version>\(.*\)</version>.*:\1:p' pom.xml | head -1)
if [[ "$new" == "$old" ]]; then
	echo "The version already is $old." >&2
	exit 1
fi

# The files that hold the version; they must have no changes of their own
files=(pom.xml '*/pom.xml' '*/META-INF/MANIFEST.MF' '*/feature.xml' '*/category.xml')
if [[ -n "$(git status --porcelain -- "${files[@]}")" ]]; then
	echo "Commit or revert the changes in these files first:" >&2
	git status --short -- "${files[@]}" >&2
	exit 1
fi

# Maven form (0.32.0, 0.32.1-SNAPSHOT) and OSGi form (0.32.0, 0.32.1.qualifier)
pattern() {
	local maven="$1" osgi="${1%-SNAPSHOT}"
	[[ "$maven" == *-SNAPSHOT ]] && osgi="$osgi.qualifier"
	printf '(%s|%s)([^0-9A-Za-z.-]|$)' "${maven//./\\.}" "${osgi//./\\.}"
}
count() { { git grep -hoE "$(pattern "$1")" -- "${files[@]}" || true; } | wc -l | tr -d ' '; }

before=$(count "$old")
echo "Setting the version from $old to $new ($before places)"

# Java 25 (see Release-HOWTO.md, Notes)
if [[ -x /usr/libexec/java_home ]] && java_25=$(/usr/libexec/java_home -v 25 2>/dev/null); then
	export JAVA_HOME="$java_25"
fi

mvn() { ./mvnw -q -B "$@"; }

echo "- OSGi modules (tycho-versions-plugin)"
mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion="$new"

for module in repl webdemo standalone.tests; do
	echo "- de.mkbauer.tinyscript.$module (versions:set)"
	mvn versions:set -DnewVersion="$new" -DgenerateBackupPoms=false \
		-f "de.mkbauer.tinyscript.$module/pom.xml"
done

echo "- the core dependency of the webdemo"
perl -0pi -e 's{(<artifactId>de\.mkbauer\.tinyscript</artifactId>\s*<version>)\Q'"$old"'\E(</version>)}{${1}'"$new"'${2}}' \
	de.mkbauer.tinyscript.webdemo/pom.xml

# Check: no old version left, the new one in as many places
left=$(count "$old")
after=$(count "$new")
if [[ "$left" -ne 0 || "$after" -ne "$before" ]]; then
	echo "Check failed: $left places still have $old, $after of $before have $new:" >&2
	git grep -nE "$(pattern "$old")" -- "${files[@]}" >&2 || true
	exit 1
fi

git diff --stat
echo "Version set to $new in $after places. Not committed."
