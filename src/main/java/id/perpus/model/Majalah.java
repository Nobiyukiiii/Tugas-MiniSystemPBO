package id.perpus.model;

public class Majalah extends Item {
    private final String edisi;

    public Majalah(String id, String judul, int tahun, String edisi) {
        super(id, judul, tahun);
        if (edisi == null || edisi.isBlank()) throw new IllegalArgumentException("Edisi wajib diisi");
        this.edisi = edisi.trim();
    }

    @Override public int getMaxLoanDays() { return 7; }
    @Override public double getDendaPerHari() { return 500; }
    @Override public String getJenis() { return "Majalah"; }
    @Override public String getDetail() { return "Edisi: " + edisi; }

    public String getEdisi() { return edisi; }
}
