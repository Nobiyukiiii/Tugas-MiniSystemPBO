package id.perpus.service;

import id.perpus.model.Anggota;
import id.perpus.model.Item;
import id.perpus.model.Peminjaman;
import id.perpus.repository.Repository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Logika bisnis perpustakaan. Bergantung pada interface Repository, bukan implementasinya. */
public class PerpustakaanService {
    private final Repository<Item> itemRepo;
    private final Repository<Anggota> anggotaRepo;
    private final List<Peminjaman> riwayat = new ArrayList<>();

    public PerpustakaanService(Repository<Item> itemRepo, Repository<Anggota> anggotaRepo) {
        this.itemRepo = itemRepo;
        this.anggotaRepo = anggotaRepo;
    }

    public void tambahItem(Item item) {
        if (itemRepo.cariById(item.getId()).isPresent())
            throw new PerpustakaanException("ID koleksi sudah terdaftar: " + item.getId());
        itemRepo.simpan(item);
    }

    public void hapusItem(String id) {
        Item item = cariItem(id);
        if (!item.isTersedia()) throw new PerpustakaanException("Koleksi sedang dipinjam, tidak bisa dihapus");
        itemRepo.hapus(id);
    }

    public Item cariItem(String id) {
        return itemRepo.cariById(id).orElseThrow(() -> new PerpustakaanException("Koleksi tidak ditemukan: " + id));
    }

    public List<Item> cariItemByJudul(String kata) {
        String q = kata.toLowerCase(Locale.ROOT);
        return itemRepo.semua().stream().filter(i -> i.getJudul().toLowerCase(Locale.ROOT).contains(q)).toList();
    }

    public List<Item> semuaItem() { return itemRepo.semua(); }

    public void tambahAnggota(Anggota anggota) {
        if (anggotaRepo.cariById(anggota.getId()).isPresent())
            throw new PerpustakaanException("ID anggota sudah terdaftar: " + anggota.getId());
        anggotaRepo.simpan(anggota);
    }

    public Anggota cariAnggota(String id) {
        return anggotaRepo.cariById(id).orElseThrow(() -> new PerpustakaanException("Anggota tidak ditemukan: " + id));
    }

    public List<Anggota> semuaAnggota() { return anggotaRepo.semua(); }

    public Peminjaman pinjam(String idAnggota, String idItem, LocalDate tanggal) {
        Anggota anggota = cariAnggota(idAnggota);
        Item item = cariItem(idItem);
        if (!item.isTersedia()) throw new PerpustakaanException("Koleksi sedang dipinjam");
        if (!anggota.bisaMeminjam())
            throw new PerpustakaanException("Kuota pinjam " + anggota.getNama() + " penuh (" + anggota.getMaxPinjam() + ")");
        Peminjaman p = new Peminjaman(anggota, item, tanggal);
        riwayat.add(p);
        return p;
    }

    public Peminjaman kembali(String idPeminjaman, LocalDate tanggal) {
        Peminjaman p = riwayat.stream()
                .filter(r -> r.getId().equalsIgnoreCase(idPeminjaman.trim()))
                .findFirst()
                .orElseThrow(() -> new PerpustakaanException("Transaksi tidak ditemukan: " + idPeminjaman));
        try {
            p.kembalikan(tanggal);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new PerpustakaanException(e.getMessage());
        }
        return p;
    }

    public List<Peminjaman> riwayat() { return Collections.unmodifiableList(riwayat); }

    public List<Peminjaman> pinjamanAktif() { return riwayat.stream().filter(Peminjaman::isAktif).toList(); }
}
