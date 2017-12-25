/*
 * Copyright (C) 2015-2016 Willi Ye <williye97@gmail.com>
 *
 * This file is part of Kernel Adiutor.
 *
 * Kernel Adiutor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Kernel Adiutor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Kernel Adiutor.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.moro.mtweaks.fragments.kernel;

import android.text.InputType;

import com.moro.mtweaks.R;
import com.moro.mtweaks.fragments.ApplyOnBootFragment;
import com.moro.mtweaks.fragments.RecyclerViewFragment;
import com.moro.mtweaks.utils.Device;
import com.moro.mtweaks.utils.Prefs;
import com.moro.mtweaks.utils.kernel.vm.VM;
import com.moro.mtweaks.utils.kernel.vm.ZRAM;
import com.moro.mtweaks.utils.kernel.vm.ZSwap;
import com.moro.mtweaks.views.recyclerview.CardView;
import com.moro.mtweaks.views.recyclerview.GenericSelectView2;
import com.moro.mtweaks.views.recyclerview.ProgressBarView;
import com.moro.mtweaks.views.recyclerview.RecyclerViewItem;
import com.moro.mtweaks.views.recyclerview.SeekBarView;
import com.moro.mtweaks.views.recyclerview.SwitchView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by willi on 29.06.16.
 */
public class VMFragment extends RecyclerViewFragment {

    private List<GenericSelectView2> mVMs = new ArrayList<>();

    private ProgressBarView swap;
    private ProgressBarView mem;

    @Override
    protected void init() {
        super.init();

        addViewPagerFragment(ApplyOnBootFragment.newInstance(this));
    }

    @Override
    protected void addItems(List<RecyclerViewItem> items) {
        mVMs.clear();

        memBarsInit(items);
        if (ZRAM.supported()) {
            zramInit(items);
        }
        zswapInit(items);
        vmTunnablesInit(items);
    }

    private void memBarsInit (List<RecyclerViewItem> items){
        CardView card = new CardView(getActivity());
        card.setTitle(getString(R.string.memory));

        long swap_total = Device.MemInfo.getItemMb("SwapTotal");
        long swap_progress = swap_total - Device.MemInfo.getItemMb("SwapFree");

        swap = new ProgressBarView();
        swap.setTitle("SWAP");
        swap.setItems(swap_total, swap_progress);
        swap.setUnit(getResources().getString(R.string.mb));
        swap.setProgressColor(getResources().getColor(R.color.blue_accent));
        card.addItem(swap);

        long mem_total = Device.MemInfo.getItemMb("MemTotal");
        long mem_progress = mem_total - (Device.MemInfo.getItemMb("Cached") + Device.MemInfo.getItemMb("MemFree"));

        mem = new ProgressBarView();
        mem.setTitle("RAM");
        mem.setItems(mem_total, mem_progress);
        mem.setUnit(getResources().getString(R.string.mb));
        mem.setProgressColor(getResources().getColor(R.color.orange_accent));
        card.addItem(mem);

        items.add(card);
    }

    private void vmTunnablesInit (List<RecyclerViewItem> items){
        CardView vmCard = new CardView(getActivity());
        vmCard.setTitle(getString(R.string.vm_tunnables));

        for (int i = 0; i < VM.size(); i++) {
            if (VM.exists(i)) {

                GenericSelectView2 vm = new GenericSelectView2();
                vm.setTitle(VM.getName(i));
                vm.setValue(VM.getValue(i));
                vm.setValueRaw(vm.getValue());
                vm.setInputType(InputType.TYPE_CLASS_NUMBER);

                final int position = i;
                vm.setOnGenericValueListener(new GenericSelectView2.OnGenericValueListener() {
                    @Override
                    public void onGenericValueSelected(GenericSelectView2 genericSelectView, String value) {
                        VM.setValue(value, position, getActivity());
                        genericSelectView.setValue(value);
                        refreshVMs();
                    }
                });

                vmCard.addItem(vm);
                mVMs.add(vm);
            }
        }

        if (vmCard.size() > 0) {
            items.add(vmCard);
        }
    }

    private void zramInit(List<RecyclerViewItem> items) {
        CardView zramCard = new CardView(getActivity());
        zramCard.setTitle(getString(R.string.zram));

        SeekBarView zram = new SeekBarView();
        zram.setTitle(getString(R.string.disksize));
        zram.setSummary(getString(R.string.disksize_summary));
        zram.setUnit(getString(R.string.mb));
        zram.setMax(2048);
        zram.setOffset(10);
        zram.setProgress(ZRAM.getDisksize() / 10);
        zram.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
            @Override
            public void onStop(SeekBarView seekBarView, int position, String value) {
                ZRAM.setDisksize(position * 10, getActivity());
            }

            @Override
            public void onMove(SeekBarView seekBarView, int position, String value) {
            }
        });

        zramCard.addItem(zram);

        if (zramCard.size() > 0) {
            items.add(zramCard);
        }
    }

    private void zswapInit(List<RecyclerViewItem> items) {
        CardView zswapCard = new CardView(getActivity());
        zswapCard.setTitle(getString(R.string.zswap));

        if (ZSwap.hasEnable()) {
            SwitchView zswap = new SwitchView();
            zswap.setTitle(getString(R.string.zswap));
            zswap.setSummary(getString(R.string.zswap_summary));
            zswap.setChecked(ZSwap.isEnabled());
            zswap.addOnSwitchListener(new SwitchView.OnSwitchListener() {
                @Override
                public void onChanged(SwitchView switchView, boolean isChecked) {
                    ZSwap.enable(isChecked, getActivity());
                }
            });

            zswapCard.addItem(zswap);
        }

        if (ZSwap.hasMaxPoolPercent()) {
            if(!Prefs.getBoolean("memory_pool_percent", false, getActivity())) {
                SeekBarView maxPoolPercent = new SeekBarView();
                maxPoolPercent.setTitle(getString(R.string.memory_pool));
                maxPoolPercent.setSummary(getString(R.string.memory_pool_summary));
                maxPoolPercent.setUnit("%");
                maxPoolPercent.setMax(ZSwap.getStockMaxPoolPercent() / 10);
                maxPoolPercent.setProgress(ZSwap.getMaxPoolPercent() / 10);
                maxPoolPercent.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                    @Override
                    public void onStop(SeekBarView seekBarView, int position, String value) {
                        ZSwap.setMaxPoolPercent(position * 10, getActivity());
                    }

                    @Override
                    public void onMove(SeekBarView seekBarView, int position, String value) {
                    }
                });

                zswapCard.addItem(maxPoolPercent);

            } else {
                SeekBarView maxPoolPercent = new SeekBarView();
                maxPoolPercent.setTitle(getString(R.string.memory_pool));
                maxPoolPercent.setSummary(getString(R.string.memory_pool_summary));
                maxPoolPercent.setUnit("%");
                maxPoolPercent.setMax(ZSwap.getStockMaxPoolPercent());
                maxPoolPercent.setProgress(ZSwap.getMaxPoolPercent());
                maxPoolPercent.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                    @Override
                    public void onStop(SeekBarView seekBarView, int position, String value) {
                        ZSwap.setMaxPoolPercent(position, getActivity());
                    }

                    @Override
                    public void onMove(SeekBarView seekBarView, int position, String value) {
                    }
                });

                zswapCard.addItem(maxPoolPercent);
            }

        }

        if (ZSwap.hasMaxCompressionRatio()) {
            SeekBarView maxCompressionRatio = new SeekBarView();
            maxCompressionRatio.setTitle(getString(R.string.maximum_compression_ratio));
            maxCompressionRatio.setSummary(getString(R.string.maximum_compression_ratio_summary));
            maxCompressionRatio.setUnit("%");
            maxCompressionRatio.setProgress(ZSwap.getMaxCompressionRatio());
            maxCompressionRatio.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                @Override
                public void onStop(SeekBarView seekBarView, int position, String value) {
                    ZSwap.setMaxCompressionRatio(position, getActivity());
                }

                @Override
                public void onMove(SeekBarView seekBarView, int position, String value) {
                }
            });

            zswapCard.addItem(maxCompressionRatio);
        }

        if (zswapCard.size() > 0) {
            items.add(zswapCard);
        }
    }

    private void refreshVMs() {
        getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mVMs.size(); i++) {
                    mVMs.get(i).setValue(VM.getValue(i));
                    mVMs.get(i).setValueRaw(mVMs.get(i).getValue());
                }
            }
        }, 250);
    }

    protected void refresh() {
        super.refresh();

        if (swap != null) {
            long total = Device.MemInfo.getItemMb("SwapTotal");
            long progress = total - Device.MemInfo.getItemMb("SwapFree");
            swap.setItems(total, progress);
        }
        if (mem != null) {
            long total = Device.MemInfo.getItemMb("MemTotal");
            long progress = total - (Device.MemInfo.getItemMb("Cached") + Device.MemInfo.getItemMb("MemFree"));
            mem.setItems(total, progress);
        }
    }

}
