package com.example.revolut.ui.main;

import android.app.LauncherActivity;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.revolut.callback.CurrencyUpdatInterface;
import com.example.revolut.data.CurrencyRepository;
import com.example.revolut.model.Currency;
import com.example.revolut.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class MainViewModel extends ViewModel {

    private final CurrencyRepository currencyRepository;
    private CompositeDisposable disposable;

    public CurrencyUpdatInterface currencyUpdatInterface;

    private String current_currency_name = "";
    private String base_code = "EUR";
    private static Double base_amount = 1.00;

    private final MutableLiveData<List<Currency>> currencies = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>();
    private final MutableLiveData<Boolean> error = new MutableLiveData<>();

    private List<Currency> finalResult = new ArrayList<>();
    private static int position = 0;

    @Inject
    public MainViewModel(CurrencyRepository currencyRepository){
        this.currencyRepository = currencyRepository;
        disposable =  new CompositeDisposable();
        loading.setValue(true);
        fetchCurrency(base_code, base_amount);
    }

    public LiveData<List<Currency>> getCurrencies() {
        return currencies;
    }

    LiveData<Boolean> getLoading() {
        return loading;
    }

    LiveData<Boolean> getErrror() {
        return error;
    }

    public void setBaseValues(Currency currency){
        Log.d("setBase", " this has been call with amount : " + currency.getExch_rate());
        fetchCurrency(currency.getCurency_code(), currency.getExch_rate());
    }

    protected void fetchCurrency(String base_code, double base_amount){

        if(base_code.equalsIgnoreCase(current_currency_name)){
            currencyUpdatInterface.onAmountUpdate(base_amount);
        }
        else{
            Log.e("Base currency", ""+ base_code + " Current currency: " + current_currency_name);
            current_currency_name = base_code;
            currencyRepository.getCurrencies()
                    .delay(1, TimeUnit.SECONDS)
                    .concatMap(response -> getModifiedObservable(response.getBase_currency(),Utils.getCurrencies(response.getResults())))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .repeatUntil(() -> !(base_code.equalsIgnoreCase(current_currency_name)))
                    .subscribe(listofcurrencies -> {
                                currencies.setValue(constructSourceList(listofcurrencies));
                                loading.setValue(false);
                                this.error.setValue(false);
                            },
                            error -> {
                                Log.e("LiveData", "with error here" + error);

                                // set to empty string so that the fetch can happen again when the retry button is clicked
                                current_currency_name = "";
                                this.error.setValue(true);
                            });
        }

    }


    protected Observable<List<Currency>> getModifiedObservable(String base_currency, List<Currency> currency) {

       return Observable.create((ObservableOnSubscribe<List<Currency>>) e -> {

           List<Currency> currencies = new ArrayList<>();

           currencies.add(new Currency(base_currency, 1.00));
           currencies.addAll(currency);

           e.onNext(currencies);
           e.onComplete();
       }).subscribeOn(Schedulers.io());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (disposable != null) {
            disposable.clear();
            disposable = null;
        }
    }

    public int getPosition(){
        return position;
    }

    public static void onCurrencyItemClicked(int pos){
        position = pos;
        Log.e("isdataMoved", "" + position);
    }

    protected List<Currency> constructSourceList(List<Currency> currencies){
        finalResult = currencies;

//        Currency currency = finalResult.get(position);
//        finalResult.remove(position);
//        finalResult.add(0, currency);
        Collections.swap(finalResult, getPosition(), 0);

        return finalResult;
    }
}
