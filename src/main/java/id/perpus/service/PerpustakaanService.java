package id.perpus.service;

import id.perpus.model.Anggota;
import id.perpus.model.Item;
import id.perpus.model.Peminjaman;
import id.perpus.repository.Repository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Logika bisnis perpustakaan. Bergantung pada interface Repository, bukan implementasinya. */
public class PerpustakaanService {
    private final Repository<Item> itemRepo;
    private final Repository<Anggota> anggotaRepo;
    private final List<Peminjaman> riwayat = new ArrayList<>();

    public PerpustakaanService(Repository<Item> itemRepo, Repository<Anggota> anggotaRepo) {
        this.itemRepo = itemRepo;
        this.anggotaRepo = anggotaRepo;
    }

    // ---------- Koleksi ----------

    public void tambahItem(Item item) {
        if (itemRepo.cariById(item.getId()) != null) {
            throw new PerpustakaanException("ID koleksi sudah terdaftar: " + item.getId());
        }
        itemRepo.simpan(item);
    }

    public void hapusItem(String id) {
        Item item = cariItem(id);
        if (!item.isTersedia()) {
            throw new PerpustakaanException("Koleksi sedang dipinjam, tidak bisa dihapus");
        }
        itemRepo.hapus(id);
    }

    public Item cariItem(String id) {
        Item item = itemRepo.cariById(id);
        if (item == null) {
            throw new PerpustakaanException("Koleksi tidak ditemukan: " + id);
        }
        return item;
    }

    public List<Item> cariItemByJudul(String kata) {
        String kataKecil = kata.toLowerCase();
        List<Item> hasil = new ArrayList<>();
        for (Item item : itemRepo.semua()) {
            if (item.getJudul().toLowerCase().contains(kataKecil)) {
                hasil.add(item);
            }
        }
        return hasil;
    }

    public List<Item> semuaItem() {
        return itemRepo.semua();
    }

    // ---------- Anggota ----------

    public void tambahAnggota(Anggota anggota) {
        if (anggotaRepo.cariById(anggota.getId()) != null) {
            throw new PerpustakaanException("ID anggota sudah terdaftar: " + anggota.getId());
        }
        anggotaRepo.simpan(anggota);
    }

    public Anggota cariAnggota(String id) {
        Anggota anggota = anggotaRepo.cariById(id);
        if (anggota == null) {
            throw new PerpustakaanException("Anggota tidak ditemukan: " + id);
        }
        return anggota;
    }

    public List<Anggota> semuaAnggota() {
        return anggotaRepo.semua();
    }

    // ---------- Transaksi ----------

    public Peminjaman pinjam(String idAnggota, String idItem, LocalDate tanggal) {
        Anggota anggota = cariAnggota(idAnggota);
        Item item = cariItem(idItem);

        if (!item.isTersedia()) {
            throw new PerpustakaanException("Koleksi sedang dipinjam");
        }
        if (!anggota.bisaMeminjam()) {
            throw new PerpustakaanException(
                    "Kuota pinjam " + anggota.getNama() + " penuh (" + anggota.getMaxPinjam() + ")");
        }

        Peminjaman peminjaman = new Peminjaman(anggota, item, tanggal);
        riwayat.add(peminjaman);
        return peminjaman;
    }

    public Peminjaman kembali(String idPeminjaman, LocalDate tanggal) {
        Peminjaman peminjaman = cariPeminjaman(idPeminjaman);
        try {
            peminjaman.kembalikan(tanggal);
        } catch (IllegalStateException e) {
            throw new PerpustakaanException(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new PerpustakaanException(e.getMessage());
        }
        return peminjaman;
    }

    private Peminjaman cariPeminjaman(String id) {
        for (Peminjaman p : riwayat) {
            if (p.getId().equalsIgnoreCase(id.trim())) {
                return p;
            }
        }
        throw new PerpustakaanException("Transaksi tidak ditemukan: " + id);
    }

    public List<Peminjaman> riwayat() {
        return Collections.unmodifiableList(riwayat);
    }

    public List<Peminjaman> pinjamanAktif() {
        List<Peminjaman> aktif = new ArrayList<>();
        for (Peminjaman p : riwayat) {
            if (p.isAktif()) {
                aktif.add(p);
            }
        }
        return aktif;
    }
}
