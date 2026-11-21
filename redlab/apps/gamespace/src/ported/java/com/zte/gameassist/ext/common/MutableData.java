package com.zte.gameassist.ext.common;

import com.zte.gameassist.ext.common.MutableData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/* loaded from: classes2.dex */
public class MutableData<T> {
    private T data;
    private final List<Observer<? super T>> observers;

    public interface Observer<T> {
        void onChanged(T t);
    }

    public MutableData() {
        this(null);
    }

    public MutableData(T t) {
        this.observers = new ArrayList();
        this.data = t;
    }

    public synchronized void setData(T t) {
        if (this.data != t) {
            this.data = t;
            notifyObservers();
        }
    }

    public synchronized T getData() {
        return this.data;
    }

    public int observeCount() {
        return this.observers.size();
    }

    public synchronized void observe(boolean z, Observer<? super T> observer) {
        if (z) {
            if (!this.observers.contains(observer)) {
                this.observers.add(observer);
                observer.onChanged(this.data);
            }
        } else {
            this.observers.remove(observer);
        }
    }

    private void notifyObservers() {
        this.observers.forEach(new Consumer() { // from class: com.zte.gameassist.ext.common.MutableData$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MutableData.this.m457x1a51cebd((MutableData.Observer) obj);
            }
        });
    }

    /* renamed from: lambda$notifyObservers$0$com-zte-gameassist-ext-common-MutableData, reason: not valid java name */
    /* synthetic */ void m457x1a51cebd(Observer observer) {
        observer.onChanged(this.data);
    }
}
