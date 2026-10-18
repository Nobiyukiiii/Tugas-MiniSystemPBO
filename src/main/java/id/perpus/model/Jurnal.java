package id.perpus.model;

public class Jurnal extends Item {
    private final String issn;

    public Jurnal(String id, String judul, int tahun, String issn) {
        super(id, judul, tahun);
        if (issn == null || issn.isBlank()) throw new IllegalArgumentException("ISSN wajib diisi");
        this.issn = issn.trim();
    }

    @Override public int getMaxLoanDays() { return 3; }
    @Override public double getDendaPerHari() { return 2_000; }
    @Override public String getJenis() { return "Jurnal"; }
    @Override public String getDetail() { return "ISSN: " + issn; }

    public String getIssn() { return issn; }
}
