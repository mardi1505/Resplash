package io.github.b_lam.resplash.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.gson.Gson;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.adapters.FooterAdapter;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.mikepenz.fastadapter_extensions.items.ProgressItem;
import com.mikepenz.fastadapter_extensions.scroll.EndlessRecyclerOnScrollListener;
import java.util.List;
import io.github.b_lam.resplash.activities.DetailActivity;
import io.github.b_lam.resplash.data.data.Photo;
import io.github.b_lam.resplash.data.service.PhotoService;
import io.github.b_lam.resplash.R;
import io.github.b_lam.resplash.Resplash;
import retrofit2.Call;
import retrofit2.Response;
import tr.xip.errorview.ErrorView;

/**
 * Created by Brandon on 10/8/2016.
 */

public class FeaturedFragment extends Fragment{

    private String TAG = "FeaturedFragment";
    private PhotoService mService;
    private FastItemAdapter<Photo> mPhotoAdapter;
    private List<Photo> mPhotos;
    private RecyclerView mImageRecycler;
    private SwipeRefreshLayout mSwipeContainer;
    private ProgressBar mImagesProgress;
    private ErrorView mImagesErrorView;
    private FooterAdapter<ProgressItem> mFooterAdapter;
    private int mPage, mColumns;
    private String mSort;

    public FeaturedFragment() {
        // Required empty public constructor
    }

    public static FeaturedFragment newInstance(String sort) {
        FeaturedFragment photoFragment = new FeaturedFragment();

        Bundle args = new Bundle();
        args.putString("sort", sort);
        photoFragment.setArguments(args);

        return photoFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Resplash.getInstance());
        String mLayoutType = sharedPreferences.getString("item_layout", "List");
        mSort = getArguments().getString("sort", "latest");
        if(mLayoutType.equals("List") || mLayoutType.equals("Cards")){
            mColumns = 1;
        }else{
            mColumns = 2;
        }

        mService = PhotoService.getService();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        setRetainInstance(true);

        mPage = 1;

        View rootView = inflater.inflate(R.layout.fragment_featured, container, false);
        mImageRecycler = (RecyclerView) rootView.findViewById(R.id.fragment_featured_recycler);
        mImagesProgress = (ProgressBar) rootView.findViewById(R.id.fragment_featured_progress);
        mImagesErrorView = (ErrorView) rootView.findViewById(R.id.fragment_featured_error_view);
        mSwipeContainer = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeContainerFeatured);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), mColumns);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        mImageRecycler.setLayoutManager(gridLayoutManager);
        mImageRecycler.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        mImageRecycler.setItemViewCacheSize(20);
        mPhotoAdapter = new FastItemAdapter<>();

        mPhotoAdapter.withOnClickListener(onClickListener);

        mFooterAdapter = new FooterAdapter<>();

        mImageRecycler.setAdapter(mFooterAdapter.wrap(mPhotoAdapter));

        mImageRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        mImageRecycler.addOnScrollListener(new EndlessRecyclerOnScrollListener(mFooterAdapter) {
            @Override
            public void onLoadMore(int currentPage) {
                mFooterAdapter.clear();
                mFooterAdapter.add(new ProgressItem().withEnabled(false));
                loadMore();
            }
        });

        mSwipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mPage = 1;
                loadMore();
            }
        });

        loadMore();
        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            mService.cancel();
        }
    }

    private FastAdapter.OnClickListener<Photo> onClickListener = new FastAdapter.OnClickListener<Photo>(){
        @Override
        public boolean onClick(View v, IAdapter<Photo> adapter, Photo item, int position) {
            Intent i = new Intent(getContext(), DetailActivity.class);
            i.putExtra("Photo", new Gson().toJson(item));
            ImageView imageView = (ImageView) v.findViewById(R.id.item_image_img);
            if(imageView.getDrawable() != null) {
                Resplash.getInstance().setDrawable(imageView.getDrawable());
            }
            startActivity(i);
            return false;
        }
    };

    public void updateAdapter(List<Photo> photos) {
        mPhotoAdapter.add(photos);
    }

    public void loadMore(){
        if(mPhotos == null){
            mImagesProgress.setVisibility(View.VISIBLE);
            mImageRecycler.setVisibility(View.GONE);
            mImagesErrorView.setVisibility(View.GONE);
        }

        Log.d(TAG, "Page " + mPage);

        PhotoService.OnRequestPhotosListener mPhotoRequestListener = new PhotoService.OnRequestPhotosListener() {
            @Override
            public void onRequestPhotosSuccess(Call<List<Photo>> call, Response<List<Photo>> response) {
                Log.d(TAG, String.valueOf(response.code()));
                if(response.code() == 200) {
                    mPhotos = response.body();
                    FeaturedFragment.this.updateAdapter(mPhotos);
                    mPage++;
                    mImagesProgress.setVisibility(View.GONE);
                    mImageRecycler.setVisibility(View.VISIBLE);
                    mImagesErrorView.setVisibility(View.GONE);
                }else{
                    mImagesErrorView.setTitle(R.string.error_http);
                    mImagesErrorView.setSubtitle(R.string.error_http_subtitle);
                    mImagesProgress.setVisibility(View.GONE);
                    mImageRecycler.setVisibility(View.GONE);
                    mImagesErrorView.setVisibility(View.VISIBLE);
                }
                if(mSwipeContainer.isRefreshing()) {
                    Toast.makeText(getContext(), "Updated images!", Toast.LENGTH_SHORT).show();
                    mSwipeContainer.setRefreshing(false);
                }
            }

            @Override
            public void onRequestPhotosFailed(Call<List<Photo>> call, Throwable t) {
                Log.d(TAG, t.toString());
                mImagesErrorView.showRetryButton(false);
                mImagesErrorView.setTitle(R.string.error_network);
                mImagesErrorView.setSubtitle(R.string.error_network_subtitle);
                mImagesProgress.setVisibility(View.GONE);
                mImageRecycler.setVisibility(View.GONE);
                mImagesErrorView.setVisibility(View.VISIBLE);
                mSwipeContainer.setRefreshing(false);
            }
        };

        mService.requestCuratedPhotos(mPage, Resplash.DEFAULT_PER_PAGE, mSort, mPhotoRequestListener);

    }
}