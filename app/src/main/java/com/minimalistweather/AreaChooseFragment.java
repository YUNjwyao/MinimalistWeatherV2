package com.minimalistweather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.minimalistweather.database_entity.City;
import com.minimalistweather.database_entity.District;
import com.minimalistweather.database_entity.Province;
import com.minimalistweather.util.HttpUtil;
import com.minimalistweather.util.JsonParser;

import org.jetbrains.annotations.NotNull;
import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AreaChooseFragment extends Fragment {

    public static final String baseAreaUrl = "http://guolin.tech/api/china/";

    /**
     * 定义地区查询类型
     */
    public static final String TYPE_PROVINCE = "province";
    public static final String TYPE_CITY = "city";
    public static final String TYPE_DISTRICT = "district";

    /**
     * 定义地区选择等级
     */
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_DISTRICT = 2;

    /**
     * 声明布局中的各个组件
     */
    private TextView mAreaTitleText;
    private Button mBackButton;

    /**
     * RecyclerView相关
     */
    private RecyclerView mRecyclerView;
    private AreaAdapter mAreaAdapter;

    /**
     * 数据相关
     */
    private List<Province> mProvinces;
    private List<City> mCities;
    private List<District> mDistricts;
    private List<String> mAreaData = new ArrayList<>();

    private Province mSelectedProvince; // 选中的省级规划
    private City mSelectedCity; // 选中的市级规划
    private int currentLevel; // 当前选中地区等级

    private ProgressDialog mProgressDialog; // 查询进度框

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_choose_area, container, false);

        /*
         * 初始化布局中的各个组件
         */
        mAreaTitleText = (TextView) view.findViewById(R.id.area_title_text);
        mBackButton = (Button) view.findViewById(R.id.back_button);

        /*
         * 初始化RecyclerView
         */
        mRecyclerView = (RecyclerView) view.findViewById(R.id.area_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAreaAdapter = new AreaAdapter(mAreaData);
        mRecyclerView.setAdapter(mAreaAdapter);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        /*
         * 点击返回按钮，返回上一级地区
         */
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel == LEVEL_DISTRICT) {
                    // 当前等级为：区县级，返回市级，查询城市
                    updateCities();
                } else if(currentLevel == LEVEL_CITY) {
                    // 当前等级为：市级，返回省级，查询省份
                    updateProvinces();
                }
            }
        });

        updateProvinces();
    }

    /**
     * 更新省份数据
     */
    private void updateProvinces() {
        mAreaTitleText.setText("中国");
        mBackButton.setVisibility(View.GONE);
        mProvinces = LitePal.findAll(Province.class); // 从数据库查询省份数据
        if(mProvinces.size() > 0) {
            // 数据库有数据，直接更新
            mAreaData.clear();
            for(Province province : mProvinces) {
                mAreaData.add(province.getProvinceName());
            }
            mAreaAdapter.notifyDataSetChanged();
            currentLevel = LEVEL_PROVINCE;
        } else {
            queryAreaFromServer(baseAreaUrl, TYPE_PROVINCE);
        }
    }

    /**
     * 更新市级数据
     */
    private void updateCities() {
        mAreaTitleText.setText(mSelectedProvince.getProvinceName());
        mBackButton.setVisibility(View.VISIBLE);
        mCities = LitePal.where("provinceid = ?", String.valueOf(mSelectedProvince.getId())).find(City.class);
        if(mCities.size() > 0) {
            mAreaData.clear();
            for(City city : mCities) {
                mAreaData.add(city.getCityName());
            }
            mAreaAdapter.notifyDataSetChanged();
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = mSelectedProvince.getProvinceCode();
            String url = baseAreaUrl + provinceCode;
            queryAreaFromServer(url, TYPE_CITY);
        }
    }

    /**
     * 更新区县数据
     */
    private void updateDistrict() {
        mAreaTitleText.setText(mSelectedCity.getCityName());
        mBackButton.setVisibility(View.VISIBLE);
        mDistricts = LitePal.where("cityid = ?", String.valueOf(mSelectedCity.getId())).find(District.class);
        if(mDistricts.size() > 0) {
            mAreaData.clear();
            for(District district : mDistricts) {
                mAreaData.add(district.getDistrictName());
            }
            mAreaAdapter.notifyDataSetChanged();
            currentLevel = LEVEL_DISTRICT;
        } else {
            int provinceCode = mSelectedProvince.getProvinceCode();
            int cityCode = mSelectedCity.getCityCode();
            String url = baseAreaUrl + provinceCode + "/" + cityCode;
            queryAreaFromServer(url, TYPE_DISTRICT);
        }
    }

    /**
     * 从服务器上查询area数据
     * @param url 请求url
     * @param type 查询类型
     */
    private void queryAreaFromServer(String url, final String type) {
        showProgress();
        HttpUtil.sendHttpRequest(url, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgress();
                        Toast.makeText(getContext(), "load failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String responseStr = response.body().string(); // 响应字符串
                boolean isParseSuccess = false; // 解析是否成功
                /*
                 * 根据type解析相应的数据
                 */
                if(TYPE_PROVINCE.equals(type)) {
                    isParseSuccess = JsonParser.parseProvinceResponse(responseStr);
                } else if(TYPE_CITY.equals(type)) {
                    isParseSuccess = JsonParser.parseCityResponse(responseStr, mSelectedProvince.getId());
                } else if(TYPE_DISTRICT.equals(type)) {
                    isParseSuccess = JsonParser.parseDistrictResponse(responseStr, mSelectedCity.getId());
                }
                if(isParseSuccess) {
                    // 如果解析成功，切换到主线程，更新数据
                    Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgress();
                            switch (type) {
                                case TYPE_PROVINCE:
                                    updateProvinces();
                                    break;
                                case TYPE_CITY:
                                    updateCities();
                                    break;
                                case TYPE_DISTRICT:
                                    updateDistrict();
                                    break;
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 显示查询进度
     */
    private void showProgress() {
        if(mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage("loading...");
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.show();
    }

    /**
     * 关闭进度框
     */
    private void closeProgress() {
        if(mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    /**
     * RecyclerView相关
     */
    private class AreaViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private String mAreaItemName;

        private TextView mAreaItemText;

        public AreaViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public AreaViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.list_item_area, parent, false));

            mAreaItemText = (TextView) itemView.findViewById(R.id.area_item_text);
            itemView.setOnClickListener(this);
        }

        public void bind(String areaItemName) {
            mAreaItemName = areaItemName;
            mAreaItemText.setText(mAreaItemName);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            if(currentLevel == LEVEL_PROVINCE) { // 当前选中等级为省，查询市
                mSelectedProvince = mProvinces.get(position);
                updateCities();
            } else if(currentLevel == LEVEL_CITY) { // 当前选中等级为市，查询区县
                mSelectedCity = mCities.get(position);
                updateDistrict();
            } else if(currentLevel == LEVEL_DISTRICT) { // 当选中项为区，跳转到天气显示
                String weatherId = mDistricts.get(position).getWeatherId();
                if(getActivity() instanceof AreaChooseActivity) {
                    Intent intent = new Intent(getActivity(), WeatherActivity.class);
                    intent.putExtra("weather_id", weatherId);
                    startActivity(intent);
                    getActivity().finish();
                } else if(getActivity() instanceof WeatherActivity) {
                    WeatherFragment fragment = (WeatherFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    fragment.drawerLayout.closeDrawers();
                    fragment.refresh.setRefreshing(false);
                    fragment.currentWeatherId = weatherId;
                    fragment.requestWeatherNow(weatherId);
                    fragment.requestWeatherAirQuality(weatherId);
                    fragment.requestWeatherForecast(weatherId);
                    fragment.requestWeatherLifestyle(weatherId);
                }

            }
        }
    }
    private class AreaAdapter extends RecyclerView.Adapter<AreaViewHolder> {

        private List<String> mAreas;

        public AreaAdapter(List<String> areas) {
            mAreas = areas;
        }

        @NonNull
        @Override
        public AreaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            return new AreaViewHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull AreaViewHolder holder, int position) {
            String areaItemName = mAreas.get(position);
            holder.bind(areaItemName);
        }

        @Override
        public int getItemCount() {
            return mAreas.size();
        }
    }
}
