package com.android.app.showdance.fragment;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.android.app.showdance.adapter.DanceVideoMainAdapter;
import com.android.app.showdance.custom.GridViewWithHeaderAndFooter;
import com.android.app.showdance.logic.VolleyManager;
import com.android.app.showdance.model.DanceVideoBean;
import com.android.app.showdance.model.glmodel.AdPicAndIconInfo.Category;
import com.android.app.showdance.model.glmodel.HotVideoListInfo;
import com.android.app.showdance.model.glmodel.HotVideoListInfo.HotVideo;
import com.android.app.showdance.model.glmodel.HotVideoListInfo.ResponseNewVideo;
import com.android.app.showdance.ui.PlayVideoActivity;
import com.android.app.showdance.utils.ConstantsUtil;
import com.android.app.showdance.utils.FormatCurrentData;
import com.android.app.showdance.utils.L;
import com.android.app.showdance.utils.NetUtil;
import com.android.app.showdance.utils.StringUtils;
import com.android.app.showdance.widget.CustomAlertDialog;
import com.android.app.wumeiniang.R;
import com.android.app.wumeiniang.app.InitApplication;
import com.andview.refreshview.XRefreshView;
import com.andview.refreshview.XRefreshView.XRefreshViewListener;
import com.facebook.drawee.view.SimpleDraweeView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 功能：最新、待审核视频
 * 
 * @author djd
 */
public class NewVideoFragment extends Fragment {

    private static final String TAG = "NewVideoFragment";
    private static final int PER_PAGE_NUMBER = 21; // 每次请求从服务器获取的视频数量
    private String mNextPageUrlMethod = "";
    private boolean mLastPage = false; // 是否处于最后一页数据标记
    private boolean isPullRefresh = true; // 是否是下拉刷新动作，不是下拉刷新，则为上拉加载更多

    private XRefreshView mXRefreshView; // 下拉、上拉刷新布局控件
    private GridViewWithHeaderAndFooter mVideoListGridView;
    private List<HotVideo> mHotVideoList = new ArrayList<HotVideoListInfo.HotVideo>(); // 通过JSON获取的视频信息集
    private List<DanceVideoBean> mDanceVideoBeanList; // 做显示数据用的数据集
    private DanceVideoMainAdapter mDanceVideoMainAdapter; // “首页”视频列表adapter

    private RelativeLayout mEmptyLayout; // 未找到视频列表的提示View

    private Dialog mDialog;

    private Category mCategory;

    // 冠军视频相关元素
    private RelativeLayout mGjLayout;
    private RelativeLayout mGjRecentLayout;
    private RelativeLayout mGjPlayAndShareLayout;
    private SimpleDraweeView mGjBackground;
    private ImageView mGjTopMedal;
    private Button mGjFlowerBtn;
    private TextView mGjTitle;
    private TextView mGjRecentTV;
    private TextView mGjPlayCountTV;
    private TextView mGjShareCountTV;
    private HotVideo mGjVideoBean;

    public static NewVideoFragment newInstance() {
        NewVideoFragment fragment = new NewVideoFragment();
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        L.i(TAG, "onResume");
        // getVideoList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nationwide, container, false);
        initView(view);
        init();
        goToTop();
        openProgressDialog();
        getVideoList();
        return view;
    }

    @SuppressLint("InflateParams")
    private void initView(View view) {
        mXRefreshView = (XRefreshView) view.findViewById(R.id.id_nationwide_custom_view);
        mVideoListGridView = (GridViewWithHeaderAndFooter) view.findViewById(R.id.id_category_gridview_video_list);

        mEmptyLayout = (RelativeLayout) LayoutInflater.from(getActivity()).inflate(R.layout.layout_empty, null);

        initChampionView(view);
    }

    @SuppressLint("InflateParams")
    private void initChampionView(View view) {
        mGjLayout = (RelativeLayout) LayoutInflater.from(getActivity()).inflate(R.layout.layout_main_gj, null);

        mGjPlayAndShareLayout = (RelativeLayout) mGjLayout.findViewById(R.id.id_gj_playcount_and_sharecount_layout);
        mGjBackground = (SimpleDraweeView) mGjLayout.findViewById(R.id.id_gj_pic_sdv);
        mGjTopMedal = (ImageView) mGjLayout.findViewById(R.id.id_gj_top_medal_iv);
        mGjFlowerBtn = (Button) mGjLayout.findViewById(R.id.id_gj_top_flower_count_btn);
        mGjTitle = (TextView) mGjLayout.findViewById(R.id.id_gj_title_tv);
        mGjRecentLayout = (RelativeLayout) mGjLayout.findViewById(R.id.id_gj_recent_layout);
        mGjRecentTV = (TextView) mGjLayout.findViewById(R.id.id_gj_recent_text_tv);
        mGjPlayCountTV = (TextView) mGjLayout.findViewById(R.id.id_gj_playcount_tv);
        mGjShareCountTV = (TextView) mGjLayout.findViewById(R.id.id_gj_share_tv);

        mGjLayout.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                openPlayVideoActivity(mGjVideoBean);
            }
        });
    }

    private void goToTop() {
        mGjLayout.setFocusable(true);
        mGjLayout.setFocusableInTouchMode(true);
        mGjLayout.requestFocus();
    }

    private void init() {
        mCategory = (Category) getArguments().getSerializable(ConstantsUtil.CATEGORY);
        L.i(TAG, "Category name is " + mCategory.getName() + ";----Category type is " + mCategory.getType());

        mXRefreshView.setPullRefreshEnable(true);
        mXRefreshView.setAutoLoadMore(true);
        mXRefreshView.setPinnedContent(true);
        mXRefreshView.setXRefreshViewListener(getXRefreshViewListener());
        mXRefreshView.setPullLoadEnable(true); // 开启上拉加载更多
        mXRefreshView.setMoveFootWhenDisablePullLoadMore(false); // 页面可被向上拉动

        initVideoListGridView();
    }

    private void initVideoListGridView() {
        mDanceVideoBeanList = new ArrayList<DanceVideoBean>();
        mVideoListGridView.addHeaderView(mEmptyLayout);
        mVideoListGridView.addHeaderView(mGjLayout);
        mDanceVideoMainAdapter = new DanceVideoMainAdapter(InitApplication.getRealContext(), mDanceVideoBeanList,
                mVideoListGridView);
        mVideoListGridView.setAdapter(mDanceVideoMainAdapter);
        mVideoListGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openPlayVideoActivity(mHotVideoList.get(Integer.parseInt(id + ""))); // 显示的视频列表从第2个开始
            }
        });
    }

    /**
     * 功能：视频播放页面（内置播放器，非网页版）
     * 
     * @param danceVideoBean
     */
    public void openPlayVideoActivity(HotVideo hotVideo) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable("hotVideo", hotVideo);
        intent.putExtras(bundle);
        intent.setClass(getActivity(), PlayVideoActivity.class);
        startActivity(intent);
    }

    private void openProgressDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = new AlertDialog.Builder(getActivity()).create();
        mDialog.show();
        // 注意此处要放在show之后 否则会报异常
        mDialog.setContentView(R.layout.loading_progressbar_dialog);
        mDialog.setCancelable(true); // false设置点击其他地方不能取消进度条
    }

    private XRefreshViewListener getXRefreshViewListener() {
        return new XRefreshView.SimpleXRefreshListener() {

            @Override
            public void onRefresh() {
                super.onRefresh();
                isPullRefresh = true;
                getVideoList();
            }

            @Override
            public void onLoadMore(boolean isSilence) {
                super.onLoadMore(isSilence);
                isPullRefresh = false;
                getVideoList();
            }

        };
    }

    private void getVideoList() {
        L.d(TAG, "getVideoList()");
        if (NetUtil.getNetworkState(InitApplication.getRealContext()) == NetUtil.NETWORN_NONE) {
            mXRefreshView.stopRefresh();
            mXRefreshView.stopLoadMore();
            Toast.makeText(InitApplication.getRealContext(), "请先连接网络！", Toast.LENGTH_SHORT).show();
            return;
        }
        if (NetUtil.isWifiConnected(InitApplication.getRealContext())) {// 已开启wifi网络
            getVideoListMethod();
        } else {// 未开启wifi网络
            new CustomAlertDialog(getActivity()).builder(R.style.DialogTVAnimWindowAnim).setTitle("网络提示")
                    .setMsg("WIFI网络未开启,是否继续使用2G、3G或4G网络!").setPositiveButton("确  认", new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getVideoListMethod();
                        }
                    }).setNegativeButton("取  消", new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mXRefreshView.stopRefresh();
                            mXRefreshView.stopLoadMore();
                        }
                    }).show();
        }
    }

    private void getVideoListMethod() {
        if (isPullRefresh) {
            getVideoListMethod_PullRefresh();
        } else {
            getVideoListMethod_LoadMore();
        }
    }

    /**
     * 功能：下拉刷新获取数据方法
     */
    private void getVideoListMethod_PullRefresh() {
        HotVideoListInfo.Request request = new HotVideoListInfo.Request(getRequestType(), "", "", PER_PAGE_NUMBER, 1);

        VolleyManager.getInstance().postRequest(request, VolleyManager.METHOD_HOT_VIDEO,
                new OnResponseListener<HotVideoListInfo.ResponseNewVideo>(HotVideoListInfo.ResponseNewVideo.class) {
                    @Override
                    protected void handleResponse(ResponseNewVideo response) {
                        if (response != null) {
                            // 判断是否还有下一批数据
                            String nextPageUrl = response.getData().getNext_page_url();
                            L.i(TAG, "下拉刷新后，当前页数是：" + response.getData().getCurrent_page());
                            L.i(TAG, "下页数据链接是：" + nextPageUrl);
                            if (nextPageUrl == null || "".equals(nextPageUrl)) {
                                mNextPageUrlMethod = "";
                                // mNextPage = 1;
                                mLastPage = true; // 已经处于最后一批数据
                            } else {
                                mNextPageUrlMethod = nextPageUrl.substring(nextPageUrl.lastIndexOf("/") + 1);
                                // mNextPage =
                                // response.getData().getCurrent_page() + 1;
                                mLastPage = false;
                            }
                            // 下拉刷新永远只取第一批21条数据
                            if (mHotVideoList.size() != 0) {
                                mHotVideoList.clear();
                            }
                            mHotVideoList = response.getData().getData();
                            L.d(TAG, "hotVideoList size is " + mHotVideoList.size());
                            for (HotVideo hotVideo : mHotVideoList) {
                                L.i(TAG, "获取的视频名称：" + hotVideo.getVideoname());
                            }
                            updateVideoListUI(mHotVideoList);
                        }
                        mXRefreshView.stopRefresh();
                        dismissDialog();
                    }
                }, mErrorListener);
    }

    /**
     * 功能：上拉加载更多数据数据方法
     */
    private void getVideoListMethod_LoadMore() {
        HotVideoListInfo.RequestNewVideo request = new HotVideoListInfo.RequestNewVideo(getRequestType(), "", "",
                PER_PAGE_NUMBER);
        // 获取当前的请求链接
        if (!mLastPage) { // 不处于最后一批视频数据时，可以接着继续请求数据
            if (!"".equals(mNextPageUrlMethod)) {
                VolleyManager.getInstance().postRequest(request, mNextPageUrlMethod,
                        new OnResponseListener<HotVideoListInfo.ResponseNewVideo>(
                                HotVideoListInfo.ResponseNewVideo.class) {
                            @Override
                            protected void handleResponse(ResponseNewVideo response) {
                                if (response != null) {
                                    // 判断是否还有下一批数据
                                    String nextPageUrl = response.getData().getNext_page_url();
                                    L.i(TAG, "上拉加载后，当前页数是：" + response.getData().getCurrent_page());
                                    L.i(TAG, "下页数据链接是：" + nextPageUrl);
                                    if (nextPageUrl == null || "".equals(nextPageUrl)) {
                                        mNextPageUrlMethod = "";
                                        mLastPage = true; // 已经处于最后一批数据
                                    } else {
                                        mNextPageUrlMethod = nextPageUrl.substring(nextPageUrl.lastIndexOf("/") + 1);
                                        mLastPage = false;
                                    }
                                    // 判断是首批数据还是新增数据
                                    List<HotVideo> hotVideoList = response.getData().getData();
                                    L.i(TAG, "获取的视频列表数量为：" + hotVideoList.size());
                                    for (HotVideo hotVideo : hotVideoList) {
                                        L.i(TAG, "获取的视频名称：" + hotVideo.getVideoname());
                                    }
                                    if (mHotVideoList.size() == 0) {
                                        mHotVideoList = hotVideoList;
                                    } else {
                                        mHotVideoList.addAll(hotVideoList);
                                    }
                                    L.d(TAG, "hotVideoList size is " + mHotVideoList.size() + 1); // 由于第一个视频作为首条视频单独显示，并移除出了Gridview数据，为了反映真实数据，这里需要+1
                                    updateVideoListUI(mHotVideoList);
                                }
                                mXRefreshView.stopLoadMore();
                                dismissDialog();
                            }
                        }, mErrorListener);
            }
        } else {
            L.d(TAG, "没有更多视频了！");
            Toast.makeText(InitApplication.getRealContext(), "没有更多视频了！", Toast.LENGTH_SHORT).show();
            mXRefreshView.stopLoadMore();
        }
    }

    private int getRequestType() {
        switch (mCategory.getType()) {
        case 0: // 全国榜分类
            return 0;
        case 4: // 最新视频分类
            return 3;
        }
        return 0;
    }

    /**
     * 功能：主页冠军视图的更新
     */
    private void updateGjUI(HotVideo hotVideo) {
        mGjVideoBean = hotVideo;

        if (mCategory.getType() == 4) { // 显示“最新视频”，则隐藏冠军标志
            mGjRecentLayout.setVisibility(View.VISIBLE);
            mGjPlayAndShareLayout.setVisibility(View.GONE);
            mGjTopMedal.setVisibility(View.GONE);
        } else {
            mGjRecentLayout.setVisibility(View.GONE);
            mGjPlayAndShareLayout.setVisibility(View.VISIBLE);
            mGjTopMedal.setVisibility(View.VISIBLE);
        }
        mGjBackground.setImageURI(mGjVideoBean.getImg());
        mGjFlowerBtn.setText(mGjVideoBean.getHot() + "");
        mGjTitle.setText(mGjVideoBean.getVideoname());
        try {
            mGjRecentTV.setText(FormatCurrentData.getTimeRange(hotVideo.getCreated_at()));
        } catch (ParseException e) {
            e.printStackTrace();
            L.e(TAG, e.getMessage());
        }
        mGjPlayCountTV.setText(StringUtils.getPlayCountStr(mGjVideoBean.getCount()));
        mGjShareCountTV.setText(StringUtils.getShareCountStr(mGjVideoBean.getShare_count()));
    }

    /**
     * 功能：首页视图更新
     * 
     * @param hotVideoList
     */
    private void updateVideoListUI(List<HotVideo> hotVideoList) {
        L.i(TAG, "updateVideoListUI");
        if (hotVideoList != null && hotVideoList.size() > 0) {
            mEmptyLayout.setVisibility(View.GONE);
            // mEmptyLayout.setPadding(0, -mEmptyLayout.getHeight(), 0, 0); //
            // 由于隐藏HeaderView后，还是会占用位置，需要把显示内容进行上移

            if (isPullRefresh) { // 只有下拉刷新和首次进入“最新视频”，才更新冠军视图
                updateGjUI(hotVideoList.get(0)); // 第一个视频时的冠军视频
                mGjLayout.setVisibility(View.VISIBLE);
                hotVideoList.remove(0); // 移除已经添加的冠军视频元素
            }

            if (hotVideoList.size() > 0) { // 只有一个视频的情况下，就只显示冠军视频，隐藏其它视频列表
                updateVideoListGridView(hotVideoList);
            }
        } else { // 收到了空的热门视频列表则置空显示
            L.d(TAG, "视频列表为空！");
            mGjLayout.setVisibility(View.GONE);
            mEmptyLayout.setVisibility(View.VISIBLE);
            // mEmptyLayout.setPadding(0, 0, 0, 0); //
            // 由于，隐藏时候上移过位置，需要恢复HeaderView的显示位置
            // 清空数据列表
            updateVideoListGridView(hotVideoList);
        }
        goToTop();
    }

    private void updateVideoListGridView(List<HotVideo> hotVideoList) {
        mDanceVideoMainAdapter.setCategoryType(mCategory.getType());
        mDanceVideoMainAdapter.setDanceVideoBeanList(getDanceVideoBeanListFromHotVideoList(hotVideoList));
        mDanceVideoMainAdapter.notifyDataSetChanged();
        mVideoListGridView.setVisibility(View.VISIBLE);
    }

    private List<DanceVideoBean> getDanceVideoBeanListFromHotVideoList(List<HotVideo> hotVideoList) {
        if (mDanceVideoBeanList != null) {
            mDanceVideoBeanList.clear();
        }
        mDanceVideoBeanList = new ArrayList<DanceVideoBean>();
        if (hotVideoList != null && hotVideoList.size() > 0) {
            for (HotVideo hotVideo : hotVideoList) {
                mDanceVideoBeanList.add(new DanceVideoBean(hotVideo.getImg(), hotVideo.getHot(), hotVideo.getHot(),
                        hotVideo.getVideoname(), hotVideo.getCreated_at(), hotVideo.getCount(),
                        hotVideo.getShare_count(), hotVideo.getName()));
            }
            // 重复添加数据，测试大数据量下是否会崩溃
            // for (HotVideo hotVideo : hotVideoList) {
            // mDanceVideoBeanList.add(new DanceVideoBean(hotVideo.getImg(),
            // hotVideo.getHot(), hotVideo.getHot(),
            // hotVideo.getVideoname(), hotVideo.getCreated_at(),
            // hotVideo.getCount(), hotVideo.getShare_count(),
            // hotVideo.getName()));
            // }
        }
        return mDanceVideoBeanList;
    }

    /**
     * 功能：过滤出特定分类需要显示的视频
     */
    @SuppressWarnings("unused")
    private List<HotVideo> getCategoryFilterVideo(List<HotVideo> hotVideoList) {
        int type = mCategory.getType();
        List<HotVideo> tempHotVideoList = new ArrayList<HotVideoListInfo.HotVideo>();
        switch (type) {
        case 0: // 显示全国榜
            L.d(TAG, "显示全国榜--type:" + type);
            tempHotVideoList = hotVideoList;
            break;
        case 1: // 显示梁海洋活动
            L.d(TAG, "显示梁海洋活动榜--type:" + type);
            for (HotVideo hotVideo : hotVideoList) {
                String theme = hotVideo.getTheme();
                L.d(TAG, "hotVideo.getTheme() is " + theme);
                if (theme != null) {
                    if (theme.contains("梁海洋")) {
                        tempHotVideoList.add(hotVideo);
                    }
                }
            }
            break;
        }
        return tempHotVideoList;
    }

    public abstract class OnResponseListener<T> extends VolleyManager.ResponeListener<T> {
        public OnResponseListener(Class<T> c) {
            super(c);
        }

        @Override
        public void onMyResponse(T response) {
            dismissDialog();
            handleResponse(response);
        }

        public void onResponseFail(com.android.app.showdance.model.glmodel.ResponseFail response) {
            L.e(TAG, "onResponseFail()");
            if (response != null) {
                L.e(TAG, response.getMessage());
            }
            dismissDialog();
            mXRefreshView.stopRefresh();
            mXRefreshView.stopLoadMore();
            updateVideoListUI(null);
            handleFailResponse(response);
        }

        protected void handleFailResponse(com.android.app.showdance.model.glmodel.ResponseFail response) {

        }

        protected abstract void handleResponse(T response);
    };

    private VolleyManager.ErrorListener mErrorListener = new VolleyManager.ErrorListener() {
        public void onErrorResponse(com.android.volley.VolleyError error) {
            L.d(TAG, "onErrorResponse----" + error.toString());
            handleErrorResponse(error);
        };
    };

    private void handleErrorResponse(com.android.volley.VolleyError error) {
        mXRefreshView.stopRefresh();
        mXRefreshView.stopLoadMore();
        dismissDialog();
    }

    private void dismissDialog() {
        L.i(TAG, "dismissDialog");
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        L.i(TAG, "onPause");
        dismissDialog();
    }
}
