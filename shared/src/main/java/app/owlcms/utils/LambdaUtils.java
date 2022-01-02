/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.util.function.Consumer;

public class LambdaUtils {
    @FunctionalInterface
    public interface ThrowingConsumer<T, E extends Throwable> {
        void accept(T t) throws E;
    }

    public static <T> Consumer<T> throwingConsumerWrapper(
            ThrowingConsumer<T, Throwable> throwingConsumer) {

        return i -> {
            try {
                // System. err.println("calling wrapped consumer " + i);
                throwingConsumer.accept(i);
            } catch (Exception ex) {
                // System. err.println("Exception " + ex);
                throw new RuntimeException(ex);
            } catch (Throwable e) {
                // System. err.println("Throwable " + e);
                throw new RuntimeException(e);
            }
        };
    }
}
