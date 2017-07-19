package pl.jermey.sourceviewer.repository;

import android.content.Context;
import android.text.TextUtils;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.android.sqlitex.SqlitexDatabaseSource;
import io.requery.meta.EntityModel;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveSupport;
import io.requery.sql.Configuration;
import io.requery.sql.ConfigurationBuilder;
import io.requery.sql.EntityDataStore;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import pl.jermey.sourceviewer.R;
import pl.jermey.sourceviewer.model.Models;
import pl.jermey.sourceviewer.model.PageSource;
import pl.jermey.sourceviewer.model.PageSourceEntity;

@EBean(scope = EBean.Scope.Singleton)
public class SourceRepository {

    private static final int DB_VERSION = 1;

    @RootContext
    Context rootContext;

    private ReactiveEntityStore<Persistable> dataStore;
    private OkHttpClient okHttpClient;

    @AfterInject
    void init() {
        EntityModel entityModel = Models.DEFAULT;
        SqlitexDatabaseSource databaseSource = new SqlitexDatabaseSource(rootContext, entityModel, DB_VERSION);
        Configuration configuration = new ConfigurationBuilder(databaseSource, entityModel)
                .build();
        dataStore = ReactiveSupport.toReactiveStore(new EntityDataStore<Persistable>(configuration));
        okHttpClient = new OkHttpClient();
    }

    public Observable<PageSource> getPageSource(String url) {
        return dataStore.select(PageSource.class)
                .where(PageSourceEntity.URL.eq(url))
                .get().observable()
                .filter(result -> !TextUtils.isEmpty(result.getSourceFile()))
                .switchIfEmpty(fetchPageSource(url))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
    }

    private Observable<PageSource> fetchPageSource(String url) {
        return Observable.fromPublisher(subscriber -> {
            Response response = null;
            Request request = new Request.Builder().url(url).get().build();
            try {
                response = okHttpClient.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {

                    String sourceFile = rootContext.getFilesDir().getAbsolutePath() + "/" + url.replace("http://", "").replace("/", "_");
                    FileOutputStream fileOutputStream = new FileOutputStream(sourceFile);
                    fileOutputStream.write(response.body().bytes());
                    fileOutputStream.close();

                    PageSourceEntity entity = new PageSourceEntity();
                    entity.setUrl(url);
                    entity.setSourceFile(sourceFile);
                    dataStore.upsert(entity).subscribe(subscriber::onNext);
                } else {
                    subscriber.onError(new Throwable(rootContext.getString(R.string.error_message, response.code(), response.message())));
                }
            } catch (IOException exception) {
                subscriber.onError(exception);
            }
        });
    }

}
