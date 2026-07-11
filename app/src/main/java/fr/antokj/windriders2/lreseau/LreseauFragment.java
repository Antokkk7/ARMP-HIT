package fr.antokj.windriders2.lreseau;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import fr.antokj.windriders2.databinding.FragmentLreseauBinding;

public class LreseauFragment extends Fragment {

    private FragmentLreseauBinding binding;
    private static final int REQUEST_WIFI_PERMISSIONS = 1001;
    private ExecutorService executorService;
    private LreseauViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLreseauBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        executorService = Executors.newFixedThreadPool(4);

        viewModel = new ViewModelProvider(requireActivity()).get(LreseauViewModel.class);

        binding.buttonReseau1.setText("Scan réseau WiFi");
        binding.buttonReseau1.setOnClickListener(v -> scanWifiNetwork());

        binding.buttonReseau2.setText("Scan appareils actifs");
        binding.buttonReseau2.setOnClickListener(v -> scanActiveDevices());

        binding.buttonReseau3.setText("Scan sous-réseau");
        binding.buttonReseau3.setOnClickListener(v -> scanSubnet());

        binding.buttonReseau4.setText("Mettre une alerte"); // à faire plus tatfd
        binding.buttonShowLastScan.setOnClickListener(v -> showLastScanResults());

        observeScanResults();
        return root;
    }

    private void showLastScanResults() {
        List<String> allIps = viewModel.getAllIps().getValue();
        List<String> usedIps = viewModel.getUsedIps().getValue();

        if (allIps == null || allIps.isEmpty()) {
            Toast.makeText(requireContext(), "Aucun scan enregistré", Toast.LENGTH_SHORT).show();
            return;
        }

        ListView listView = new ListView(requireContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                allIps
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                String ip = getItem(position);
                text.setText(ip);
                if (usedIps.contains(ip)) {
                    text.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                } else {
                    text.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
                }
                return view;
            }
        };
        showListDialog("Derniers résultats du scan (" + allIps.get(0).substring(0, allIps.get(0).lastIndexOf('.')) + ".0/24)", listView, adapter);
    }

    private void observeScanResults() {
        // vide
    }

    // BOUTON 1 : scan réseau wifi de l'appareil
    private void scanWifiNetwork() {
        if (!checkPermissions()) {
            requestPermissions();
            return;
        }

        WifiManager wifiManager = (WifiManager) requireContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            Toast.makeText(requireContext(), "Wifi non disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            Toast.makeText(requireContext(), "Non connecté à un réseau WiFi", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder info = new StringBuilder();
        info.append("=== Informations réseau WiFi ===\n\n");
        info.append("Nom de la connexion : ").append(wifiInfo.getSSID()).append("\n");
        info.append("SSID : ").append(wifiInfo.getSSID()).append("\n");
        info.append("Adresse MAC : ").append(wifiInfo.getMacAddress()).append("\n");
        info.append("IPv4 : ").append(intToIp(wifiInfo.getIpAddress())).append("\n");
        info.append("Vitesse : ").append(wifiInfo.getLinkSpeed()).append(" Mbps\n");
        info.append("Type de sécurité : ").append(getSecurityType()).append("\n");
        info.append("Bande fréquence : ").append(getFrequencyBand(wifiInfo.getFrequency())).append("\n");
        info.append("Type d'attribution : DHCP (par défaut)\n");
        info.append("Protocole : ").append(getProtocol(wifiInfo)).append("\n");
        info.append("IPv6 : ").append(getIPv6Address()).append("\n");
        info.append("Adresse physique : ").append(getMacAddress()).append("\n");

        showResultDialog("Informations réseau WiFi", info.toString());
    }

    private String intToIp(int ip) {
        return String.format("%d.%d.%d.%d",
                (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
    }

    private String getSecurityType() {
        WifiManager wifiManager = (WifiManager) requireContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) return "Inconnu";

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) return "Inconnu";

        String currentSSID = wifiInfo.getSSID();
        if (currentSSID == null) return "Inconnu";

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return "Permission manquante";
        }

        try {
            wifiManager.startScan();
            List<android.net.wifi.ScanResult> results = wifiManager.getScanResults();

            if (results == null) return "Inconnu";

            for (android.net.wifi.ScanResult result : results) {
                if (("\"" + result.SSID + "\"").equals(currentSSID)) {
                    String caps = result.capabilities;

                    if (caps.contains("WPA3")) return "WPA3";
                    if (caps.contains("WPA2")) return "WPA2";
                    if (caps.contains("WPA")) return "WPA";
                    if (caps.contains("WEP")) return "WEP";
                    return "Open (Aucune sécurité)";
                }
            }
        } catch (SecurityException e) {
            return "Permission refusée";
        }

        return "Inconnu";
    }

    private String getFrequencyBand(int frequency) {
        if (frequency >= 2400 && frequency <= 2500) return "2.4 GHz";
        if (frequency >= 5000 && frequency <= 6000) return "5 GHz";
        return "Inconnu";
    }

    private String getProtocol(WifiInfo wifiInfo) {
        return "802.11" + (wifiInfo.getFrequency() >= 5000 ? "ac" : "n");
    }

    private String getIPv6Address() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (intf.getName().equalsIgnoreCase("wlan0")) {
                    for (java.net.InetAddress addr : Collections.list(intf.getInetAddresses())) {
                        if (addr.getAddress().length == 16) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "Non disponible";
    }

    private String getMacAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (intf.getName().equalsIgnoreCase("wlan0")) {
                    byte[] mac = intf.getHardwareAddress();
                    if (mac != null) {
                        StringBuilder sb = new StringBuilder();
                        for (byte b : mac) {
                            sb.append(String.format("%02X:", b));
                        }
                        return sb.substring(0, sb.length() - 1);
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "Non disponible";
    }

    // BOUTON 2 : Scan des appareils actifs dans le réseau
    private void scanActiveDevices() {
        if (!checkPermissions()) {
            requestPermissions();
            return;
        }

        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(requireContext());
        progressDialog.setTitle("Scan des appareils");
        progressDialog.setMessage("Recherche en cours...");
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(254);
        progressDialog.setProgress(0);
        progressDialog.setCancelable(false);
        progressDialog.show();

        executorService.execute(() -> {
            List<LreseauViewModel.DeviceInfo> devices = scanDevicesInSubnet();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    viewModel.setDevices(devices);
                    if (devices.isEmpty() || devices.size() == 1) {
                        Toast.makeText(requireContext(), "Aucun autre appareil trouvé", Toast.LENGTH_SHORT).show();
                    } else {
                        showDeviceList(devices);
                    }
                });
            }
        });
    }

    private List<LreseauViewModel.DeviceInfo> scanDevicesInSubnet() {
        List<LreseauViewModel.DeviceInfo> devices = new ArrayList<>();
        String myIp = getLocalIpAddress();
        if (myIp == null) return devices;

        String subnet = myIp.substring(0, myIp.lastIndexOf('.'));
        String myMac = getLocalMacAddress();
        devices.add(new LreseauViewModel.DeviceInfo(myMac, myIp, "Cet appareil (Vous)", true));

        ExecutorService threadPool = Executors.newFixedThreadPool(20);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 1; i <= 254; i++) {
            String ip = subnet + "." + i;
            if (ip.equals(myIp)) continue;

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    InetAddress inet = InetAddress.getByName(ip);
                    if (inet.isReachable(200)) {
                        synchronized (devices) {
                            devices.add(new LreseauViewModel.DeviceInfo(
                                    "MAC non disponible",
                                    ip,
                                    getDeviceName(ip),
                                    false
                            ));
                        }
                    }
                } catch (Exception ignored) {}
            }, threadPool);
            futures.add(future);
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }

        return devices;
    }

    private String getLocalIpAddress() {
        try {
            for (NetworkInterface intf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && addr.getAddress().length == 4) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getLocalMacAddress() {
        try {
            for (NetworkInterface intf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (intf.getName().equalsIgnoreCase("wlan0")) {
                    byte[] mac = intf.getHardwareAddress();
                    if (mac != null) {
                        StringBuilder sb = new StringBuilder();
                        for (byte b : mac) {
                            sb.append(String.format("%02X:", b));
                        }
                        return sb.substring(0, sb.length() - 1);
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "Non disponible";
    }

    private String getMacFromArpCache(String ip) {
        try {
            ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            Network activeNetwork = cm.getActiveNetwork();
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
            if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "Non disponible (root requis)";
            }
        } catch (Exception e) {
            return "Non disponible";
        }
        return "Non disponible";
    }

    private String getDeviceName(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            String hostName = addr.getHostName();
            if (hostName.equals(ip)) {
                return "Appareil (" + ip + ")";
            }
            return hostName;
        } catch (Exception e) {
            return "Appareil (" + ip + ")";
        }
    }

    private void showDeviceList(List<LreseauViewModel.DeviceInfo> devices) {
        ListView listView = new ListView(requireContext());
        ArrayAdapter<LreseauViewModel.DeviceInfo> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                devices
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                LreseauViewModel.DeviceInfo device = getItem(position);
                text1.setText(device.name);
                text2.setText(device.ip + " | " + (device.mac != null ? device.mac : "MAC inconnue"));

                if (device.isCurrentDevice) {
                    text1.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
                    text2.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
                }
                return view;
            }
        };
        showListDialog("Appareils actifs sur le réseau", listView, adapter);
    }

    // BOUTON 3 : Scan sous-réseau
    private void scanSubnet() {
        if (!checkPermissions()) {
            requestPermissions();
            return;
        }

        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(requireContext());
        progressDialog.setTitle("Scan du sous-réseau");
        progressDialog.setMessage("Recherche des IPs actives...");
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(255);
        progressDialog.setProgress(0);
        progressDialog.setCancelable(false);
        progressDialog.show();
        viewModel.setIsScanning(true);

        executorService.execute(() -> {
            String myIp = getLocalIpAddress();
            if (myIp == null) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        viewModel.setIsScanning(false);
                        Toast.makeText(requireContext(), "Impossible de récupérer l'IP locale", Toast.LENGTH_SHORT).show();
                    });
                }
                return;
            }

            String subnet = myIp.substring(0, myIp.lastIndexOf('.'));
            List<String> allIps = new ArrayList<>();
            List<String> usedIps = new ArrayList<>();

            for (int i = 0; i <= 255; i++) {
                String ip = subnet + "." + i;
                allIps.add(ip);

                try {
                    InetAddress inet = InetAddress.getByName(ip);
                    if (inet.isReachable(200)) {
                        usedIps.add(ip);
                    }
                } catch (Exception ignored) {}

                final int progress = i;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> progressDialog.setProgress(progress));
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    viewModel.setIsScanning(false);
                    viewModel.setAllIps(allIps);
                    viewModel.setUsedIps(usedIps);
                    showSubnetScanResult(allIps, usedIps);
                });
            }
        });
    }

    private void showSubnetScanResult(List<String> allIps, List<String> usedIps) {

        String myIp = getLocalIpAddress();

        ListView listView = new ListView(requireContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                allIps
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                String ip = getItem(position);
                text.setText(ip);

                if (ip.equals(myIp)) {
                    text.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
                }
                else if (usedIps.contains(ip)) {
                    text.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                }
                else {
                    text.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
                }
                return view;
            }
        };
        showListDialog("Sous-réseau (" + allIps.get(0).substring(0, allIps.get(0).lastIndexOf('.')) + ".0/24)", listView, adapter);
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                requireActivity(),
                new String[]{
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_NETWORK_STATE
                },
                REQUEST_WIFI_PERMISSIONS
        );
    }

    private void showResultDialog(String title, String message) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showListDialog(String title, ListView listView, ArrayAdapter<?> adapter) {
        listView.setAdapter(adapter);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle(title)
                .setView(listView)
                .setPositiveButton("OK", null)
                .show();
    }

    public void clearViewModelData() {
        viewModel.clearScanResults();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        executorService.shutdown();
    }
}