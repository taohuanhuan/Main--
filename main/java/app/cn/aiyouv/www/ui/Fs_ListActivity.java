package app.cn.aiyouv.www.ui;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;

import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import app.cn.aiyouv.www.R;
import app.cn.aiyouv.www.adapter.FSAdapter;
import app.cn.aiyouv.www.base.AppManager;
import app.cn.aiyouv.www.base.BaseActivity;
import app.cn.aiyouv.www.bean.Article_List;
import app.cn.aiyouv.www.bean.Fs_Bean;
import app.cn.aiyouv.www.common.C;
import app.cn.aiyouv.www.config.IUV;
import app.cn.aiyouv.www.config.U;
import app.cn.aiyouv.www.http.AsyncHttp;
import app.cn.aiyouv.www.http.ResponHandler;
import app.cn.aiyouv.www.utils.SharedUtils;
import app.cn.aiyouv.www.widget.CommonFriendPop;
import app.cn.aiyouv.www.widget.DelSlideListView;
import app.cn.aiyouv.www.widget.NewDataToast;
import app.cn.aiyouv.www.widget.OnDeleteListioner;
import app.cn.aiyouv.www.widget.PullToRefreshView;

/**
 * Created by Administrator on 2016/3/21.
 */
public class Fs_ListActivity extends BaseActivity {
    private DelSlideListView content_list;
    private FSAdapter fsAdapter;
    private PullToRefreshView refresh;
    private ArrayList<Fs_Bean> fs_beans;
    private ImageView add_friend;
    private int CURRENT_PAGE = 1;
    private int TOTAL_PAGER = -1;
    private PullToRefreshView.OnHeaderRefreshListener listHeadListener = new PullToRefreshView.OnHeaderRefreshListener() {

        @Override
        public void onHeaderRefresh(PullToRefreshView view) {
            // TODO Auto-generated method stub
            refresh.postDelayed(new Runnable() {

                @Override
                public void run() {

                    CURRENT_PAGE = 1;
                    loadData(false);

                }
            }, 1000);
        }
    };
    private PullToRefreshView.OnFooterRefreshListener listFootListener = new PullToRefreshView.OnFooterRefreshListener() {

        @Override
        public void onFooterRefresh(PullToRefreshView view) {
            // TODO Auto-generated method stub
            refresh.postDelayed(new Runnable() {

                @Override
                public void run() {

                    CURRENT_PAGE = CURRENT_PAGE+1;

                    if(CURRENT_PAGE>TOTAL_PAGER){
                        handler.sendEmptyMessage(1);
                        NewDataToast.makeText(Fs_ListActivity.this, "没有数据了").show();
                    }else{
                        loadData(true);
                    }
                }
            }, 1000);
        }
    };
    private Handler handler = new Handler(){
        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            switch (msg.what){
                case 1:

                    refresh.onHeaderRefreshComplete();
                    refresh.onFooterRefreshComplete();

                    if(msg.obj!=null) {
                        final ArrayList<Fs_Bean> fs_beans = (ArrayList<Fs_Bean>) msg.obj;
                        fsAdapter.putData(fs_beans);
                        fsAdapter.notifyDataSetChanged();
                        content_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                Intent intent = new Intent(Fs_ListActivity.this, Chat_Activity.class);
                                intent.putExtra("obj", fs_beans.get(i));
                                startActivity(intent);
                            }
                        });
                    }
                    break;
            }
        }
    };
    private OnDeleteListioner deleteListioner = new OnDeleteListioner() {
        @Override
        public boolean isCandelete(int position) {
            return true;
        }

        @Override
        public void onDelete(Object obj) {

        }
        @Override
        public void onBack() {

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fs_list_layout);
        findViewById(R.id.title_top_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AppManager.getAppManager().finishActivity();
            }
        });
        add_friend = (ImageView) findViewById(R.id.add_friend);
        content_list = (DelSlideListView) findViewById(R.id.content_list);
        fs_beans = new ArrayList<Fs_Bean>();
        fsAdapter = new FSAdapter(getApplicationContext(),fs_beans,handler,deleteListioner);
        content_list.setAdapter(fsAdapter);
        refresh = (PullToRefreshView) findViewById(R.id.refresh_layout);
        refresh.setOnHeaderRefreshListener(listHeadListener);
        refresh.setOnFooterRefreshListener(listFootListener);

        loadData(true);
        add_friend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommonFriendPop.showSheet(Fs_ListActivity.this, new CommonFriendPop.onSelect() {
                    @Override
                    public void onClick(Object object) {

                    }
                }, null);
            }
        });
    }
    private void loadData(final boolean isContinue){
        RequestParams params = new RequestParams();
        SharedUtils utils = new SharedUtils(getApplicationContext(), IUV.status);
        params.put("userId",utils.getStringValue("id"));
        params.put("appKey",utils.getStringValue("key"));

        AsyncHttp.post(U.URL+U.IUV_FS_LIST,params,new ResponHandler(Fs_ListActivity.this){
            @Override
            public void onSuccess(int index, String result) {
                super.onSuccess(index, result);
                C.p(result);
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    String status = jsonObject.getString("type");
                    if (status.equals(U.SUCCESS)) {
                        JSONObject obj = jsonObject.getJSONObject("data");
                        JSONArray jsonArray = obj.getJSONArray("content");
                        TOTAL_PAGER = obj.getInt("totalPages");
                        CURRENT_PAGE = obj.getInt("pageNumber");
                        int len = jsonArray.length();
                        C.p("列表" + len);
                        if (!isContinue) {
                            fs_beans.clear();
//                            fs_beans = new ArrayList<Fs_Bean>();
                        }
                        for(int i=0;i<len;i++){
                            //----
                            JSONObject o = jsonArray.getJSONObject(i);
                            Fs_Bean bean = new Fs_Bean();
                            bean.setId(o.getString("id"));
                            bean.setName(o.getString("name"));
                            bean.setPic(o.getString("image"));
                            fs_beans.add(bean);
                        }
                        Message msg = new Message();
                        msg.what = 1;
                        msg.obj = fs_beans;
                        handler.sendMessage(msg);

                    }else if(U.UNLOGIN.equals(status)){
                        NewDataToast.makeText(getApplicationContext(),jsonObject.getString("content")).show();
                        SharedUtils utils = new SharedUtils(getApplicationContext(), IUV.status);
                        IUV.iuv = "";
                        utils.clear();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
