package pl.jermey.sourceviewer;

import android.Manifest;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.pddstudio.highlightjs.HighlightJsView;
import com.pddstudio.highlightjs.models.Language;
import com.pddstudio.highlightjs.models.Theme;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.io.File;

import pl.jermey.sourceviewer.repository.SourceRepository;

@EActivity(R.layout.activity_main)
public class MainActivity extends RxAppCompatActivity {

    @Bean
    SourceRepository sourceRepository;

    @ViewById
    EditText urlInput;
    @ViewById
    Button loadButton;
    @ViewById
    HighlightJsView highlightView;
    @ViewById
    FrameLayout progressOverlay;

    @AfterViews
    void afterViews() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(granted -> {
                    if (granted) {
                        loadButton.setEnabled(true);
                    } else {
                        loadButton.setEnabled(false);
                        new MaterialDialog.Builder(this)
                                .title(R.string.error)
                                .content(R.string.missing_storage_permission)
                                .positiveText(R.string.ok)
                                .show();
                    }
                });
        highlightView.setHighlightLanguage(Language.HTML);
        highlightView.setTheme(Theme.SOLARIZED_LIGHT);
        highlightView.setOnContentChangedListener(() -> progressOverlay.setVisibility(View.GONE));
    }

    @Click(R.id.loadButton)
    void load() {
        String url = urlInput.getText().toString();
        if (TextUtils.isEmpty(url)) {
            new MaterialDialog.Builder(this)
                    .title(R.string.error)
                    .content(R.string.empty_url)
                    .positiveText(R.string.ok)
                    .show();
            return;
        }
        progressOverlay.setVisibility(View.VISIBLE);
        Uri uri = Uri.parse(url);
        if (TextUtils.isEmpty(uri.getScheme())) {
            url = "http://" + url;
        }
        sourceRepository.getPageSource(url)
                .compose(bindToLifecycle())
                .subscribe(pageSource -> {
                            File file = new File(pageSource.getSourceFile());
                            highlightView.setSource(file);
                        },
                        throwable -> {
                            progressOverlay.setVisibility(View.GONE);
                            new MaterialDialog.Builder(this)
                                    .title(R.string.error)
                                    .content(throwable.getMessage())
                                    .positiveText(R.string.ok).show();
                        });
    }
}
