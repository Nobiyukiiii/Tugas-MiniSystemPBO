package id.perpus.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Abstraksi anggota perpustakaan. Kuota dan bonus masa pinjam ditentukan subclass. */
public abstract class Anggota implements Identifiable {
    private final String id;
    private String nama;
    private final List<Peminjaman> pinjamanAktif = new ArrayList<>();

    protected Anggota(String id, String nama) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("ID anggota wajib diisi");
        this.id = id.trim();
        setNama(nama);
    }

    public abstract int getMaxPinjam();

    public abstract int getBonusHari();

    public abstract String getJenis();

    public boolean bisaMeminjam() {
        return pinjamanAktif.size() < getMaxPinjam();
    }

    void tambahPinjaman(Peminjaman p) { pinjamanAktif.add(p); }

    void hapusPinjaman(Peminjaman p) { pinjamanAktif.remove(p); }

    public List<Peminjaman> getPinjamanAktif() { return Collections.unmodifiableList(pinjamanAktif); }

    @Override
    public String getId() { return id; }

    public String getNama() { return nama; }

    public void setNama(String nama) {
        if (nama == null || nama.isBlank()) throw new IllegalArgumentException("Nama wajib diisi");
        this.nama = nama.trim();
    }

    @Override
    public String toString() {
        return String.format("[%s] %-9s %-20s pinjaman %d/%d",
                id, getJenis(), nama, pinjamanAktif.size(), getMaxPinjam());
    }
}
