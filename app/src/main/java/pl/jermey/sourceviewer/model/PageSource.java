package pl.jermey.sourceviewer.model;

import android.os.Parcelable;

import io.requery.Entity;
import io.requery.Key;
import io.requery.Persistable;

@Entity
public interface PageSource extends Parcelable, Persistable {
    @Key
    String getUrl();

    String getSourceFile();
}
