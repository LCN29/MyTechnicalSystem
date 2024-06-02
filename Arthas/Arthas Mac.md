
# Arthas

## arthas 存放位置

cd /opt/armors/arthas
java -jar arthas-boot.jar 

## 断点方法

watch com.seewo.care.easicare.api.service.SchoolsServiceApi getGradeInfoOfSchoolId "{params,returnObj,throwExp}" -e -x 2

## 查看方法的调用链

trace com.flashshan.certificate.web.controller.CertificateController createCertificate


# Mac 快捷键

## 强制退出程序
command + option + esc  