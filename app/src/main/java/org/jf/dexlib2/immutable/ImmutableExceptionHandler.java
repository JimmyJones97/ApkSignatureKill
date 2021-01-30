package org.jf.dexlib2.immutable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import org.jf.dexlib2.base.BaseExceptionHandler;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.util.ImmutableConverter;

public class ImmutableExceptionHandler extends BaseExceptionHandler implements ExceptionHandler {
    private static final ImmutableConverter<ImmutableExceptionHandler, ExceptionHandler> CONVERTER =
            new ImmutableConverter<ImmutableExceptionHandler, ExceptionHandler>() {
                @Override
                protected boolean isImmutable(@NonNull ExceptionHandler item) {
                    return item instanceof ImmutableExceptionHandler;
                }

                @NonNull
                @Override
                protected ImmutableExceptionHandler makeImmutable(@NonNull ExceptionHandler item) {
                    return ImmutableExceptionHandler.of(item);
                }
            };
    @Nullable
    protected final String exceptionType;
    protected final int handlerCodeAddress;

    public ImmutableExceptionHandler(@Nullable String exceptionType,
                                     int handlerCodeAddress) {
        this.exceptionType = exceptionType;
        this.handlerCodeAddress = handlerCodeAddress;
    }

    public static ImmutableExceptionHandler of(ExceptionHandler exceptionHandler) {
        if (exceptionHandler instanceof ImmutableExceptionHandler) {
            return (ImmutableExceptionHandler) exceptionHandler;
        }
        return new ImmutableExceptionHandler(
                exceptionHandler.getExceptionType(),
                exceptionHandler.getHandlerCodeAddress());
    }

    @NonNull
    public static ImmutableList<ImmutableExceptionHandler> immutableListOf(
            @Nullable Iterable<? extends ExceptionHandler> list) {
        return CONVERTER.toList(list);
    }

    @Nullable
    @Override
    public String getExceptionType() {
        return exceptionType;
    }

    @Override
    public int getHandlerCodeAddress() {
        return handlerCodeAddress;
    }
}
