package id.perpus.model;

/** Abstraksi koleksi perpustakaan. Aturan durasi dan denda ditentukan subclass. */
public abstract class Item implements Identifiable {
    private final String id;
    private String judul;
    private int tahun;
    private boolean tersedia = true;

    protected Item(String id, String judul, int tahun) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("ID koleksi wajib diisi");
        this.id = id.trim();
        setJudul(judul);
        setTahun(tahun);
    }

    public abstract int getMaxLoanDays();

    public abstract double getDendaPerHari();

    public abstract String getJenis();

    public abstract String getDetail();

    public double hitungDenda(long hariTerlambat) {
        return hariTerlambat > 0 ? hariTerlambat * getDendaPerHari() : 0;
    }

    @Override
    public String getId() { return id; }

    public String getJudul() { return judul; }

    public void setJudul(String judul) {
        if (judul == null || judul.isBlank()) throw new IllegalArgumentException("Judul wajib diisi");
        this.judul = judul.trim();
    }

    public int getTahun() { return tahun; }

    public void setTahun(int tahun) {
        if (tahun < 1450 || tahun > java.time.Year.now().getValue())
            throw new IllegalArgumentException("Tahun terbit tidak valid: " + tahun);
        this.tahun = tahun;
    }

    public boolean isTersedia() { return tersedia; }

    public void setTersedia(boolean tersedia) { this.tersedia = tersedia; }

    @Override
    public String toString() {
        return String.format("[%s] %-8s %-32s %d | %s | %s",
                id, getJenis(), judul, tahun, getDetail(), tersedia ? "Tersedia" : "Dipinjam");
    }
}
