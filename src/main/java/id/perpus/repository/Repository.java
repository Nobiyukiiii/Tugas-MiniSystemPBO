package id.perpus.repository;

import id.perpus.model.Identifiable;
import java.util.List;

/**
 * Kontrak penyimpanan data. T adalah tipe data apa saja yang punya ID
 * (contoh: Item atau Anggota).
 */
public interface Repository<T extends Identifiable> {

    /** Menyimpan data. Jika ID sudah ada, data lama ditimpa. */
    void simpan(T entitas);

    /** Mencari berdasarkan ID. Mengembalikan null jika tidak ditemukan. */
    T cariById(String id);

    /** Mengembalikan seluruh data. */
    List<T> semua();

    /** Menghapus data. Mengembalikan true jika datanya ada. */
    boolean hapus(String id);
}
