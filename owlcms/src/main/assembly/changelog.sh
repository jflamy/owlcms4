gren c -f templog.md
egrep '(^#)|(^[[:space:]]*\* )' templog.md  | awk '/(-alpha)|(-beta)|(-rc)/ {next} /4.1.15/ {exit} {print} ' > CHANGELOG.md
rm -f templog.md
