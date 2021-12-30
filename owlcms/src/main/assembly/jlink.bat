@rem ***************************************************************************
@rem Copyright (c) 2009-2022 Jean-François Lamy
@rem
@rem Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
@rem License text at https://opensource.org/licenses/NPOSL-3.0
@rem ***************************************************************************
C:\Dev\Java\jdk-11.0.5.10-hotspot\bin\jlink --no-header-files --no-man-pages --compress=2 --strip-debug --add-modules java.base,java.desktop,java.sql,jdk.unsupported,java.naming,jdk.zipfs,java.management,jdk.charsets,java.instrument --output java-runtime
C:\Dev\git\owlcms4\owlcms\target\java-runtime\bin\java.exe -DdemoMode=true -jar .\owlcms.jar
C:\Users\JF\owlcms4\jre\bin\java.exe -DdemoMode=true -jar C:\Dev\git\owlcms4\owlcms\target\owlcms.jar