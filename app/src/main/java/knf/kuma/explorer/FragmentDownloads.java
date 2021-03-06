package knf.kuma.explorer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.database.CacheDB;

public class FragmentDownloads extends FragmentBase {
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.progress)
    ProgressBar progress;
    @BindView(R.id.error)
    View error;
    private boolean isFirst = true;

    public FragmentDownloads() {
    }

    public static FragmentDownloads get() {
        return new FragmentDownloads();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        CacheDB.INSTANCE.downloadsDAO().getActive().observe(this, downloadObjects -> {
            progress.setVisibility(View.GONE);
            error.setVisibility(downloadObjects.size() == 0 ? View.VISIBLE : View.GONE);
            if (isFirst || downloadObjects.size() == 0 || (recyclerView.getAdapter() != null && downloadObjects.size() > recyclerView.getAdapter().getItemCount())) {
                isFirst = false;
                recyclerView.setAdapter(new DownloadingAdapter(FragmentDownloads.this, downloadObjects));
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.recycler_downloading, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }
}
