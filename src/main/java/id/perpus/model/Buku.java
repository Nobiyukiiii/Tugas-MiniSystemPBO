package id.perpus.model;

public class Buku extends Item {
    private final String penulis;

    public Buku(String id, String judul, int tahun, String penulis) {
        super(id, judul, tahun);
        if (penulis == null || penulis.isBlank()) throw new IllegalArgumentException("Penulis wajib diisi");
        this.penulis = penulis.trim();
    }

    @Override public int getMaxLoanDays() { return 14; }
    @Override public double getDendaPerHari() { return 1_000; }
    @Override public String getJenis() { return "Buku"; }
    @Override public String getDetail() { return "Penulis: " + penulis; }

    public String getPenulis() { return penulis; }
}
