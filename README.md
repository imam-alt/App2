# App2 Distance Meter

Aplikasi Android sederhana untuk:
1. Ambil foto
2. Menetapkan objek referensi yang ukurannya diketahui
3. Mengestimasi ukuran objek lain pada foto yang sama
4. Menghitung jarak kamera ke objek

## Cara kerja
- Pengguna mengambil foto.
- Pengguna mengetuk 2 titik pada objek referensi.
- Pengguna memasukkan ukuran nyata referensi dalam cm.
- Aplikasi menghitung skala piksel-ke-cm.
- Pengguna mengetuk 2 titik pada objek target.
- Aplikasi mengestimasi ukuran target dari rasio piksel.
- Aplikasi menghitung jarak dengan pendekatan pinhole camera:

`distance_cm = focal_length_px * actual_size_cm / object_size_px`

## Catatan akurasi
- Hasil terbaik jika objek referensi dan objek target berada pada bidang yang sama.
- Hasil jarak adalah estimasi, bergantung pada kamera belakang utama dan parameter sensor perangkat.
- Perspektif, distorsi lensa, dan sudut pengambilan foto memengaruhi hasil.

## Struktur utama
- `MainActivity.kt` -> alur foto, referensi, estimasi ukuran, estimasi jarak
- `MeasurementImageView.kt` -> tampilan gambar dan pemilihan 2 titik
- `activity_main.xml` -> UI sederhana

## Membuka project
1. Clone repo ini di Android Studio.
2. Biarkan Gradle sync.
3. Jalankan pada perangkat Android.

## Pengembangan lanjutan yang disarankan
- Ganti capture intent ke CameraX agar parameter kamera lebih konsisten.
- Tambahkan auto edge detection untuk bantu pilih garis objek.
- Tambahkan koreksi perspektif dengan homography.
- Tambahkan mode AR / depth bila perangkat mendukung.
