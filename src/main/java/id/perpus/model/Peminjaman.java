package id.perpus.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Relasi antara satu Anggota dan satu Item pada rentang waktu tertentu. */
public class Peminjaman {
    private static int counter = 0;

    private final String id;
    private final Anggota anggota;
    private final Item item;
    private final LocalDate tglPinjam;
    private final LocalDate jatuhTempo;
    private LocalDate tglKembali;
    private double denda;

    public Peminjaman(Anggota anggota, Item item, LocalDate tglPinjam) {
        this.id = String.format("TRX-%03d", ++counter);
        this.anggota = anggota;
        this.item = item;
        this.tglPinjam = tglPinjam;
        this.jatuhTempo = tglPinjam.plusDays(item.getMaxLoanDays() + anggota.getBonusHari());
        item.setTersedia(false);
        anggota.tambahPinjaman(this);
    }

    /** Menutup peminjaman dan mengembalikan denda keterlambatan. */
    public double kembalikan(LocalDate tanggal) {
        if (!isAktif()) throw new IllegalStateException("Peminjaman " + id + " sudah dikembalikan");
        if (tanggal.isBefore(tglPinjam))
            throw new IllegalArgumentException("Tanggal kembali tidak boleh sebelum tanggal pinjam");
        this.tglKembali = tanggal;
        this.denda = item.hitungDenda(ChronoUnit.DAYS.between(jatuhTempo, tanggal));
        item.setTersedia(true);
        anggota.hapusPinjaman(this);
        return denda;
    }

    public boolean isAktif() { return tglKembali == null; }

    public String getId() { return id; }
    public Anggota getAnggota() { return anggota; }
    public Item getItem() { return item; }
    public LocalDate getTglPinjam() { return tglPinjam; }
    public LocalDate getJatuhTempo() { return jatuhTempo; }
    public LocalDate getTglKembali() { return tglKembali; }
    public double getDenda() { return denda; }

    @Override
    public String toString() {
        return String.format("%s | %s -> %s | pinjam %s, tempo %s | %s",
                id, anggota.getNama(), item.getJudul(), tglPinjam, jatuhTempo,
                isAktif() ? "AKTIF" : "kembali " + tglKembali + ", denda Rp" + (long) denda);
    }
}
