package fr.antokj.windriders2.lreseau;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LreseauViewModel extends ViewModel {
    // Données du scan de sous-réseau
    private final MutableLiveData<List<String>> allIps = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> usedIps = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isScanning = new MutableLiveData<>(false);

    // Données des appareils actifs
    private final MutableLiveData<List<DeviceInfo>> devices = new MutableLiveData<>(new ArrayList<>());

    // El famoso Getters
    public LiveData<List<String>> getAllIps() { return allIps; }
    public LiveData<List<String>> getUsedIps() { return usedIps; }
    public LiveData<Boolean> getIsScanning() { return isScanning; }
    public LiveData<List<DeviceInfo>> getDevices() { return devices; }

    // El famoso Setters
    public void setAllIps(List<String> ips) { allIps.setValue(ips); }
    public void setUsedIps(List<String> ips) { usedIps.setValue(ips); }
    public void setIsScanning(boolean scanning) { isScanning.setValue(scanning); }
    public void setDevices(List<DeviceInfo> devicesList) { devices.setValue(devicesList); }

    public static class DeviceInfo {
        public String mac;
        public String ip;
        public String name;
        public boolean isCurrentDevice;

        public DeviceInfo(String mac, String ip, String name, boolean isCurrentDevice) {
            this.mac = mac;
            this.ip = ip;
            this.name = name;
            this.isCurrentDevice = isCurrentDevice;
        }
    }
}