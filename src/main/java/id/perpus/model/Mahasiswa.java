package id.perpus.model;

public class Mahasiswa extends Anggota {
    public Mahasiswa(String id, String nama) { super(id, nama); }

    @Override public int getMaxPinjam() { return 3; }
    @Override public int getBonusHari() { return 0; }
    @Override public String getJenis() { return "Mahasiswa"; }
}
