package mr_immortalz.com.modelqq;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.datatype.BmobDate;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UpdateListener;
import master.flame.danmaku.danmaku.model.Danmaku;
import mr_immortalz.com.modelqq.been.Chat;
import mr_immortalz.com.modelqq.been.Info;
import mr_immortalz.com.modelqq.been.user;
import mr_immortalz.com.modelqq.custom.CustomViewPager;
import mr_immortalz.com.modelqq.custom.RadarViewGroup;
import mr_immortalz.com.modelqq.slideWord.BarrageRelativeLayout;
import mr_immortalz.com.modelqq.tools.LocationTools;
import mr_immortalz.com.modelqq.utils.FixedSpeedScroller;
import mr_immortalz.com.modelqq.utils.LogUtil;
import mr_immortalz.com.modelqq.utils.ZoomOutPageTransformer;

public class MainActivity extends Activity implements ViewPager.OnPageChangeListener, RadarViewGroup.IRadarClickListener {

    private CustomViewPager viewPager;
    private RelativeLayout ryContainer;
    private RadarViewGroup radarViewGroup;
    private LocationManager manager;
    private ViewpagerAdapter mAdapter;
    private String lastCreatedAt = "2016-12-10 11:07:37";
    private BarrageRelativeLayout mBarrageRelativeLayout;
    private double lat;
    private double lon;
    private Location location;
    private int[] mImgs = {R.drawable.len, R.drawable.leo, R.drawable.lep,
            R.drawable.leq, R.drawable.ler, R.drawable.les, R.drawable.mln, R.drawable.mmz, R.drawable.mna,
            R.drawable.mnj, R.drawable.leo, R.drawable.leq, R.drawable.les, R.drawable.lep};
    private String[] mNames = {"小红", "唐马儒", "王尼玛", "张全蛋", "蛋花", "王大锤", "叫兽", "哆啦A梦"};
    private int mPosition;
    private FixedSpeedScroller scroller;
    private SparseArray<Info> mDatas = new SparseArray<>();
    private ArrayList<String> otherID = new ArrayList<String>();
    private boolean isFirst = true;
    private user thisUser;
    private Handler han, hanChat;
    private LinkedList<String> texts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");//设置日期格式
        lastCreatedAt=df.format(new Date());// new Date()为获取当前系统时间
        lastCreatedAt=lastCreatedAt+" 00:00:00";
        Bmob.initialize(this, "1b2551067b01b0765269eb6f4c4efd2c");
        thisUser = new user();
        otherID.add(thisUser.getUser_id());
        addData("我", 0.0);
        getGps();
        initView();
        initData();
        initSlide();

        /**
         * 将Viewpager所在容器的事件分发交给ViewPager
         */
        ryContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return viewPager.dispatchTouchEvent(event);
            }
        });
//        初始化handler
        han = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
//                toast("我在监听"+msg.what);
                searchAround();
                Log.e("test", "...han");
                getChat();
                return false;
            }
        });
        hanChat = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
//                监听弹幕

                return false;
            }
        });

        changeView();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                radarViewGroup.setDatas(mDatas);
            }
        }, 1500);
        radarViewGroup.setiRadarClickListener(this);

    }

    private void changeView() {
        mAdapter = new ViewpagerAdapter();
        viewPager.setAdapter(mAdapter);
        //设置缓存数为展示的数目
        viewPager.setOffscreenPageLimit(mDatas.size());
        viewPager.setPageMargin(getResources().getDimensionPixelOffset(R.dimen.viewpager_margin));
        //设置切换动画
        viewPager.setPageTransformer(true, new ZoomOutPageTransformer());
        viewPager.addOnPageChangeListener(this);
        setViewPagerSpeed(250);

    }

    private void initSlide() {
        mBarrageRelativeLayout = (BarrageRelativeLayout) findViewById(R.id.barrageView);
        texts = new LinkedList<String>();
        texts.add("我是萌萌的弹幕~");
        texts.add("这是沈立凡和曹德福的计网课设~");
        mBarrageRelativeLayout.setBarrageTexts(texts);
        mBarrageRelativeLayout.show(BarrageRelativeLayout.RANDOM_SHOW);
        getChat();

    }


    private void initData() {

//        for (int i = 0; i < mImgs.length; i++) {
//            Info info = new Info();
//            info.setPortraitId(mImgs[i]);
//            info.setAge(((int) Math.random() * 25 + 16) + "岁");
//            info.setName(mNames[(int) (Math.random() * mNames.length)]);
//            info.setSex(i % 3 == 0 ? false : true);
//            info.setDistance(Math.round((Math.random() * 10) * 100) / 100);
//            mDatas.put(i, info);
//            addData("我自己", (double) 0);
//        }


    }

    private void initView() {
        viewPager = (CustomViewPager) findViewById(R.id.vp);
        radarViewGroup = (RadarViewGroup) findViewById(R.id.radar);
        ryContainer = (RelativeLayout) findViewById(R.id.ry_container);
    }

    private void getGps() {
        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //如果要用GPS就把下面的NETWORK_PROVIDER改成GPS_PROVIDER,但是GPS不稳定
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        location = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 1, locationLinstener);
    }
    LocationListener locationLinstener=new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (isFirst){
                loadLocation(location);
                isFirst=false;
            }

        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
        @Override
        public void onProviderEnabled(String provider) {
        }
        @Override
        public void onProviderDisabled(String provider) {
        }
    };
    private void loadLocation(Location location) {
        lat=location.getLatitude();
        lon=location.getLongitude();
//        这里要上传位置信息并且得到周围的人
        thisUser.setUser_id((lat+"").substring(2,4)+(lon+"").substring(2,4)+(Math.random()*(10000)));
        thisUser.setName(mNames[new Random().nextInt(mNames.length-1)]);
        thisUser.setLat(lat+"");
        thisUser.setLon(lon+"");
        thisUser.save(new SaveListener<String>() {
            @Override
            public void done(String objectId,BmobException e) {
                if(e==null){
                    toast("你的临时姓名："+thisUser.getName());
                    searchThread();
                }else{
                    toast("创建数据失败：" + e.getMessage());
                }
            }
        });
    }

    private void searchThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
                        han.sendEmptyMessage(1);
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();

    }

    private void searchAround() {
        BmobQuery<user> query = new BmobQuery<user>();
//返回50条数据，如果不加上这条语句，默认返回10条数据
        query.setLimit(50);
//执行查询方法
        query.findObjects(new FindListener<user>() {
            @Override
            public void done(List<user> object, BmobException e) {
                if(e==null){
                    for (user user : object) {
                        //获得playerName的信息
                        Double otherLat= Double.valueOf(user.getLat());
                        Double otherLon= Double.valueOf(user.getLon());
//                        得到距离
                        Double dis=LocationTools.getDistance(lon,lat,otherLon,otherLat);
                        if (dis<2000&&!thisUser.getUser_id().equals(user.getUser_id())){
                            if (!isInOtherId(user.getUser_id())){
                                addData(user.getName(),dis);
                                otherID.add(user.getUser_id());
//                                han.sendEmptyMessage(1);
                                mAdapter.notifyDataSetChanged();

                            }
                        }
                    }

                }else{
                    Log.i("bmob","失败："+e.getMessage()+","+e.getErrorCode());

                }
            }
        });
    }

    private boolean isInOtherId(String id) {
        boolean b=false;
        for (int i=0;i<otherID.size();i++){
            if (id.equals(otherID.get(i))){
                b=true;
            }
        }
        return b;
    }

    private void addData(String name,Double dis) {
        Info info = new Info();
        info.setPortraitId(mImgs[(int) (1+Math.random()*12)]);
        info.setAge(((int) Math.random() * 25 + 16) + "岁");
        info.setName(name);
        int a= (int) ((Math.random()*10)%2);
        info.setSex(a == 0 ? false : true);
        info.setDistance((float) (dis/1000));
        mDatas.put(mDatas.size(), info);
        Log.e("test","info.getSex()....."+a);
    }

    private void toast(String s) {
        Toast.makeText(MainActivity.this,s,Toast.LENGTH_SHORT).show();
    }

    /**
     * 设置ViewPager切换速度
     *
     * @param duration
     */
    private void setViewPagerSpeed(int duration) {
        try {
            Field field = ViewPager.class.getDeclaredField("mScroller");
            field.setAccessible(true);
            scroller = new FixedSpeedScroller(MainActivity.this, new AccelerateInterpolator());
            field.set(viewPager, scroller);
            scroller.setmDuration(duration);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mPosition = position;
    }

    @Override
    public void onPageSelected(int position) {
        radarViewGroup.setCurrentShowItem(position);
        LogUtil.m("当前位置 " + mPosition);
        LogUtil.m("速度 " + viewPager.getSpeed());
        //当手指左滑速度大于2000时viewpager右滑（注意是item+2）
        if (viewPager.getSpeed() < -1800) {

            viewPager.setCurrentItem(mPosition + 2);
            LogUtil.m("位置 " + mPosition);
            viewPager.setSpeed(0);
        } else if (viewPager.getSpeed() > 1800 && mPosition > 0) {
            //当手指右滑速度大于2000时viewpager左滑（注意item-1即可）
            viewPager.setCurrentItem(mPosition - 1);
            LogUtil.m("位置 " + mPosition);
            viewPager.setSpeed(0);
        }
        Danmaku danmaku=new Danmaku("nihai");
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onRadarItemClick(int position) {
        viewPager.setCurrentItem(position);
    }


    class ViewpagerAdapter extends PagerAdapter {
        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            final Info info = mDatas.get(position);
            //设置一大堆演示用的数据，麻里麻烦~~
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.viewpager_layout, null);
            ImageView ivPortrait = (ImageView) view.findViewById(R.id.iv);
            ImageView ivSex = (ImageView) view.findViewById(R.id.iv_sex);
            TextView tvName = (TextView) view.findViewById(R.id.tv_name);
            TextView tvDistance = (TextView) view.findViewById(R.id.tv_distance);
            tvName.setText(info.getName());
            tvDistance.setText(info.getDistance() + "km");
            ivPortrait.setImageResource(info.getPortraitId());
            if (info.getSex()) {
                ivSex.setImageResource(R.drawable.girl);
            } else {
                ivSex.setImageResource(R.drawable.boy);
            }
            ivPortrait.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "这是 " + info.getName() + " >.<", Toast.LENGTH_SHORT).show();
                    if (info.getName().equals("我")){
                        showMessageDialog();

                    }
                }
            });
            container.addView(view);
            return view;
        }

        @Override
        public int getCount() {
            return mDatas.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        toast(isFirst+"");
        super.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        thisUser.delete(new UpdateListener() {

            @Override
            public void done(BmobException e) {
                if(e==null){
                    toast("删除临时用户:"+thisUser.getName());
                }else{
                    toast("退出");
                }
            }

        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        thisUser.delete(new UpdateListener() {

            @Override
            public void done(BmobException e) {
                if(e==null){
                    toast("退出并删除临时用户:"+thisUser.getName());
                }else{
                    toast("退出");
                }
            }

        });
    }

    private void showMessageDialog() {
        //弹出框
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        //    通过LayoutInflater来加载一个xml的布局文件作为一个View对象
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_message, null);
        //    设置我们自己定义的布局文件作为弹出框的Content
        builder.setView(view);
        final EditText editMsg= (EditText) view.findViewById(R.id.edit_msg);

        builder.setPositiveButton("发送", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                loadChat(editMsg.getText().toString());
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();
    }

    private void loadChat(String content) {
        Chat chat = new Chat();
        chat.setUser_id(thisUser.getUser_id());
        chat.setContent(content);
        chat.save(new SaveListener<String>() {
            @Override
            public void done(String objectId,BmobException e) {
                if(e==null){
                    toast("发送成功");
                }else{
                    toast("发送失败");
                }
            }
        });
    }

    private void getChat(){
        Log.e("test","...getchat");
        BmobQuery<Chat> query = new BmobQuery<Chat>();
//返回50条数据，如果不加上这条语句，默认返回10条数据
        query.setLimit(50);
//执行查询方法

//        query.addWhereContainedIn("user_id", Arrays.asList(otherID));
//        String[] ids = new String[otherID.size()];
//        for (int i=0;i<otherID.size();i++){
//            ids[i]=otherID.get(i);
//        }
//        query.addWhereContainedIn("user_id", Arrays.asList(ids));
        Log.e("test","...lastCreatedAt"+lastCreatedAt);
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date lastDate = sdf.parse(lastCreatedAt);
            query.addWhereGreaterThanOrEqualTo("createdAt",new BmobDate(lastDate));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        query.findObjects(new FindListener<Chat>() {
            @Override
            public void done(List<Chat> object, BmobException e) {
                if(e==null){
                    texts.clear();
                    texts.add("");

                    for (Chat chat : object) {
                        chat.getContent();
                        Log.e("test","chat.getCreatedAt()"+chat.getCreatedAt()+"..."+chat.getContent());
                        lastCreatedAt=chat.getCreatedAt();
                        texts.add(chat.getContent());
                    }
                    if (texts.size()>1){
                        Log.e("test","......1");
                        mBarrageRelativeLayout.setBarrageTexts(texts);
                        Log.e("test","......2");
                        mBarrageRelativeLayout.show(BarrageRelativeLayout.RANDOM_SHOW);
                        Log.e("test","......3");
                    }



                }else{
                    Log.i("test","失败："+e.getMessage()+","+e.getErrorCode());
                }
//                getChat();
            }
        });
    }
}
