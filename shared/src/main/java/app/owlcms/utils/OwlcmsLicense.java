package app.owlcms.utils;

import java.io.IOException;
import java.time.LocalDate;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import javax0.license3j.Feature;
import javax0.license3j.License;
import javax0.license3j.io.IOFormat;
import javax0.license3j.io.LicenseReader;

public class OwlcmsLicense {
    Logger logger = (Logger) LoggerFactory.getLogger(OwlcmsLicense.class);

    LicenseReader lr;

    byte[] publicKey = new byte[] {
            (byte) 0x52,
            (byte) 0x53, (byte) 0x41, (byte) 0x00, (byte) 0x30, (byte) 0x81, (byte) 0x9F, (byte) 0x30, (byte) 0x0D,
            (byte) 0x06, (byte) 0x09, (byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7, (byte) 0x0D,
            (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x05, (byte) 0x00, (byte) 0x03, (byte) 0x81, (byte) 0x8D,
            (byte) 0x00, (byte) 0x30, (byte) 0x81, (byte) 0x89, (byte) 0x02, (byte) 0x81, (byte) 0x81, (byte) 0x00,
            (byte) 0xD1, (byte) 0xEE, (byte) 0xAF, (byte) 0x5B, (byte) 0x01, (byte) 0x0B, (byte) 0x99, (byte) 0xC3,
            (byte) 0x54, (byte) 0x96, (byte) 0x10, (byte) 0x3A, (byte) 0xE3, (byte) 0xBA, (byte) 0xEA, (byte) 0x36,
            (byte) 0x6C, (byte) 0xAD, (byte) 0x2B, (byte) 0x02, (byte) 0x50, (byte) 0xF6, (byte) 0x1F, (byte) 0xFF,
            (byte) 0xE9, (byte) 0xA7, (byte) 0x06, (byte) 0xAF, (byte) 0x62, (byte) 0x2B, (byte) 0x64, (byte) 0x8F,
            (byte) 0x55, (byte) 0x17, (byte) 0xA2, (byte) 0x4C, (byte) 0x77, (byte) 0x3A, (byte) 0x08, (byte) 0x77,
            (byte) 0x05, (byte) 0x9B, (byte) 0xF9, (byte) 0x8B, (byte) 0xB9, (byte) 0xB5, (byte) 0xFB, (byte) 0x37,
            (byte) 0xAE, (byte) 0xB4, (byte) 0x3F, (byte) 0x7A, (byte) 0x82, (byte) 0xDD, (byte) 0xF0, (byte) 0x51,
            (byte) 0xBA, (byte) 0xDA, (byte) 0xE6, (byte) 0xAD, (byte) 0x29, (byte) 0xBA, (byte) 0x7C, (byte) 0xE2,
            (byte) 0x02, (byte) 0xC5, (byte) 0xF4, (byte) 0x1E, (byte) 0x43, (byte) 0x16, (byte) 0x91, (byte) 0x50,
            (byte) 0x2E, (byte) 0xF5, (byte) 0xF1, (byte) 0x98, (byte) 0x26, (byte) 0x2C, (byte) 0xF8, (byte) 0x6D,
            (byte) 0x69, (byte) 0x96, (byte) 0xD5, (byte) 0x8A, (byte) 0xF7, (byte) 0x79, (byte) 0x2C, (byte) 0xF9,
            (byte) 0xD6, (byte) 0x6F, (byte) 0x47, (byte) 0xF1, (byte) 0x99, (byte) 0x8D, (byte) 0xF1, (byte) 0x95,
            (byte) 0x45, (byte) 0x29, (byte) 0x3D, (byte) 0x42, (byte) 0x3F, (byte) 0x02, (byte) 0x26, (byte) 0xB4,
            (byte) 0x59, (byte) 0xB8, (byte) 0x48, (byte) 0xBC, (byte) 0x2F, (byte) 0x58, (byte) 0xAD, (byte) 0x95,
            (byte) 0x15, (byte) 0x16, (byte) 0xCF, (byte) 0x65, (byte) 0x0C, (byte) 0x0B, (byte) 0xEF, (byte) 0x99,
            (byte) 0xCF, (byte) 0xAD, (byte) 0x1F, (byte) 0x6C, (byte) 0xD8, (byte) 0x65, (byte) 0x6E, (byte) 0x07,
            (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x00, (byte) 0x01,
    };

    private License license;

    public boolean isFeatureAllowed(String featureName) {
        try {
            lr = new LicenseReader(ResourceWalker.getFileOrResource("license.txt"));
            license = lr.read(IOFormat.STRING);
            if (license.isOK(publicKey)) {
                logger.debug("license ok");
                Feature ef = license.get("expiry");
                if (ef != null) {
                    String dateString = ef.getString();
                    String substring = dateString.substring(dateString.indexOf("STRING:")+7);
                    try {
                        LocalDate expiry = LocalDate.parse(substring);
                        if (LocalDate.now().isAfter(expiry)) {
                            logger./**/warn("license expired on {}", expiry);
                            return false;
                        }
                    } catch (Exception e) {
                        return false;
                    }
                } else {
                    logger./**/warn("invalid license, no expiry date");
                    return false;
                }
                Feature f = license.get(featureName);
                if (f != null) {
                    boolean b = f.getString().contentEquals("true");
                    logger.debug("feature {} '{}' {}",featureName, f.getString(), b);
                    return b;
                }
            }
            return false;
        } catch (IOException e) {
            logger.debug("license not found");
            return false;
        }

    }
}
