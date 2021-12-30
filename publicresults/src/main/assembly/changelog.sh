#*******************************************************************************
# Copyright (c) 2009-2022 Jean-François Lamy
#
# Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
# License text at https://opensource.org/licenses/NPOSL-3.0
#*******************************************************************************
gren c -f templog.md
egrep '(^#)|(^[[:space:]]*\* )' templog.md  | awk '/(-alpha)|(-beta)|(-rc)/ {next} /4.1.15/ {exit} {print} ' > CHANGELOG.md
rm -f templog.md
