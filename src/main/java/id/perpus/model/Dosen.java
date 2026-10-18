package id.perpus.model;

public class Dosen extends Anggota {
    public Dosen(String id, String nama) { super(id, nama); }

    @Override public int getMaxPinjam() { return 5; }
    @Override public int getBonusHari() { return 7; }
    @Override public String getJenis() { return "Dosen"; }
}
