package fr.antokj.windriders2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import fr.antokj.windriders2.databinding.ActivityMainBinding;
import fr.antokj.windriders2.lreseau.LreseauViewModel;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedTheme();
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .setAnchorView(R.id.fab).show();
            }
        });
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_lreseau, R.id.nav_mreseau, R.id.nav_whatever)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_theme) {
            showThemeDialog();
            return true;
        }
        else if (item.getItemId() == R.id.action_clear) {
            showClearDataDialog();
            return true;
        }
        else if (item.getItemId() == R.id.action_project) {
            openProjectUrl();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showClearDataDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Effacer les données")
                .setMessage("Voulez-vous vraiment effacer toutes les données enregistrées ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    clearAllData();

                    new ViewModelProvider(this).get(LreseauViewModel.class).clearScanResults();

                    recreate();
                    Toast.makeText(this, "Données effacées", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Non", null)
                .show();
    }

    private void clearAllData() {
        // Clear le choix de thème
        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().clear().apply();
        // Clear les scans
        getSharedPreferences("ScanPrefs", MODE_PRIVATE).edit().clear().apply();
    }

    private void openProjectUrl() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Antokkk7/ARMP-HIT"));
        startActivity(browserIntent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("isDarkTheme", false);
        if (isDark) {
            setTheme(R.style.Theme_Windriders2_NoActionBar_Dark);
        } else {
            setTheme(R.style.Theme_Windriders2_NoActionBar_Light);
        }
    }

    private void saveThemePreference(boolean isDark) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("isDarkTheme", isDark).apply();
    }

    private void showThemeDialog() {
        String[] themes = {"Mode clair", "Mode sombre"};
        new AlertDialog.Builder(this)
                .setTitle("Choisir le thème")
                .setItems(themes, (dialog, which) -> {
                    boolean isDark = (which == 1);
                    saveThemePreference(isDark);
                    recreate();
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) { // Ne fonctionne plus anyway
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission refusée. Certaines fonctionnalités ne seront pas disponibles.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            Toast.makeText(this, "Permissions accordées !", Toast.LENGTH_SHORT).show();
        }
    }
}