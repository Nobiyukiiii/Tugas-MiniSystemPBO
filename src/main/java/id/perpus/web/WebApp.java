package id.perpus.web;

import static id.perpus.web.Html.esc;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import id.perpus.model.*;
import id.perpus.repository.InMemoryRepository;
import id.perpus.service.PerpustakaanException;
import id.perpus.service.PerpustakaanService;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Lapisan web. Tugasnya hanya menerima request browser, memanggil PerpustakaanService,
 * lalu menampilkan hasilnya sebagai HTML. Tidak ada aturan bisnis di sini.
 */
public class WebApp implements HttpHandler {
    private final PerpustakaanService service =
            new PerpustakaanService(new InMemoryRepository<>(), new InMemoryRepository<>());

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        WebApp app = new WebApp();
        app.isiDataAwal();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", app); // semua request ditangani method handle() milik app
        server.start();
        System.out.println("Perpustakaan berjalan di http://localhost:" + port + " (Ctrl+C untuk berhenti)");
    }

    private void isiDataAwal() {
        service.tambahItem(new Buku("B001", "Clean Code", 2008, "Robert C. Martin"));
        service.tambahItem(new Buku("B002", "Laskar Pelangi", 2005, "Andrea Hirata"));
        service.tambahItem(new Majalah("M001", "Tempo Edisi Khusus", 2023, "No. 12"));
        service.tambahItem(new Jurnal("J001", "Jurnal Informatika", 2022, "1978-1234"));
        service.tambahAnggota(new Mahasiswa("A001", "Citra Maharani"));
        service.tambahAnggota(new Dosen("D001", "Bayu Pranata"));
    }

    // ---------- Menerima request ----------

    /** Dipanggil server untuk setiap request yang masuk. */
    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();

            if (method.equals("GET") && path.equals("/")) {
                Map<String, String> query = Html.parse(ex.getRequestURI().getRawQuery());
                send(ex, 200, halaman(query));
            } else if (method.equals("POST")) {
                String isiBody = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String hasil = aksi(path, Html.parse(isiBody));
                redirect(ex, hasil);
            } else {
                send(ex, 404, "Halaman tidak ditemukan");
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            send(ex, 500, "Terjadi kesalahan server");
        }
    }

    /** Menjalankan aksi POST. Mengembalikan pesan hasil untuk alamat redirect (ok=... atau err=...). */
    private String aksi(String path, Map<String, String> form) {
        try {
            if (path.equals("/item/tambah")) {
                service.tambahItem(buatItem(form));
            } else if (path.equals("/item/hapus")) {
                service.hapusItem(isi(form, "id"));
            } else if (path.equals("/anggota/tambah")) {
                service.tambahAnggota(buatAnggota(form));
            } else if (path.equals("/pinjam")) {
                service.pinjam(isi(form, "anggota"), isi(form, "item"), tanggal(isi(form, "tanggal")));
            } else if (path.equals("/kembali")) {
                Peminjaman p = service.kembali(isi(form, "trx"), tanggal(isi(form, "tanggal")));
                return "ok=" + enc("Dikembalikan. Denda: " + rupiah(p.getDenda()));
            } else {
                throw new PerpustakaanException("Aksi tidak dikenal");
            }
            return "ok=" + enc("Berhasil");
        } catch (PerpustakaanException e) {
            return "err=" + enc(e.getMessage());
        } catch (NumberFormatException e) {
            return "err=" + enc("Tahun harus berupa angka");
        } catch (IllegalArgumentException e) {
            return "err=" + enc(e.getMessage());
        } catch (DateTimeParseException e) {
            return "err=" + enc("Format tanggal tidak valid");
        }
    }

    /** Mengambil isi form. Jika tidak ada, hasilnya teks kosong (bukan null). */
    private static String isi(Map<String, String> form, String kunci) {
        String nilai = form.get(kunci);
        return nilai == null ? "" : nilai;
    }

    private Item buatItem(Map<String, String> form) {
        String jenis = isi(form, "jenis");
        String id = isi(form, "id");
        String judul = isi(form, "judul");
        int tahun = Integer.parseInt(isi(form, "tahun"));
        String extra = isi(form, "extra");

        if (jenis.equals("buku")) {
            return new Buku(id, judul, tahun, extra);
        } else if (jenis.equals("majalah")) {
            return new Majalah(id, judul, tahun, extra);
        } else if (jenis.equals("jurnal")) {
            return new Jurnal(id, judul, tahun, extra);
        }
        throw new IllegalArgumentException("Jenis koleksi tidak valid");
    }

    private Anggota buatAnggota(Map<String, String> form) {
        String jenis = isi(form, "jenis");
        String id = isi(form, "id");
        String nama = isi(form, "nama");

        if (jenis.equals("mahasiswa")) {
            return new Mahasiswa(id, nama);
        } else if (jenis.equals("dosen")) {
            return new Dosen(id, nama);
        }
        throw new IllegalArgumentException("Jenis anggota tidak valid");
    }

    /** Teks kosong berarti hari ini. */
    private static LocalDate tanggal(String teks) {
        if (teks.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(teks); // format yyyy-MM-dd
    }

    // ---------- Membuat halaman ----------

    private String halaman(Map<String, String> query) {
        String kataCari = query.containsKey("q") ? query.get("q") : "";

        StringBuilder h = new StringBuilder(HEAD);
        h.append("<h1>Perpustakaan Mini</h1>");
        if (query.containsKey("ok")) {
            h.append("<p class='ok'>").append(esc(query.get("ok"))).append("</p>");
        }
        if (query.containsKey("err")) {
            h.append("<p class='err'>").append(esc(query.get("err"))).append("</p>");
        }
        h.append(bagianKoleksi(kataCari));
        h.append(bagianAnggota());
        h.append(bagianPeminjaman());
        h.append(bagianRiwayat());
        h.append("</body></html>");
        return h.toString();
    }

    private String bagianKoleksi(String kataCari) {
        List<Item> daftar;
        if (kataCari.isBlank()) {
            daftar = service.semuaItem();
        } else {
            daftar = service.cariItemByJudul(kataCari);
        }

        StringBuilder h = new StringBuilder();
        h.append("<section><h2>Koleksi</h2>");
        h.append("<form method='get' class='row'>");
        h.append("<input name='q' placeholder='Cari judul' value='").append(esc(kataCari)).append("'>");
        h.append("<button>Cari</button></form>");
        h.append("<table><tr><th>ID</th><th>Jenis</th><th>Judul</th><th>Tahun</th>");
        h.append("<th>Detail</th><th>Status</th><th></th></tr>");
        for (Item item : daftar) {
            String status = item.isTersedia() ? "Tersedia" : "Dipinjam";
            h.append("<tr><td>").append(esc(item.getId())).append("</td>");
            h.append("<td>").append(item.getJenis()).append("</td>");
            h.append("<td>").append(esc(item.getJudul())).append("</td>");
            h.append("<td>").append(item.getTahun()).append("</td>");
            h.append("<td>").append(esc(item.getDetail())).append("</td>");
            h.append("<td>").append(status).append("</td>");
            h.append("<td>").append(tombolHapus(item.getId())).append("</td></tr>");
        }
        if (daftar.isEmpty()) {
            h.append("<tr><td colspan='7'>Tidak ada koleksi</td></tr>");
        }
        h.append("</table>").append(FORM_ITEM).append("</section>");
        return h.toString();
    }

    private String bagianAnggota() {
        StringBuilder h = new StringBuilder();
        h.append("<section><h2>Anggota</h2>");
        h.append("<table><tr><th>ID</th><th>Jenis</th><th>Nama</th><th>Pinjaman</th></tr>");
        for (Anggota a : service.semuaAnggota()) {
            h.append("<tr><td>").append(esc(a.getId())).append("</td>");
            h.append("<td>").append(a.getJenis()).append("</td>");
            h.append("<td>").append(esc(a.getNama())).append("</td>");
            h.append("<td>").append(a.getPinjamanAktif().size()).append("/").append(a.getMaxPinjam());
            h.append("</td></tr>");
        }
        h.append("</table>").append(FORM_ANGGOTA).append("</section>");
        return h.toString();
    }

    private String bagianPeminjaman() {
        StringBuilder h = new StringBuilder();
        h.append("<section><h2>Peminjaman</h2>");
        h.append("<form method='post' action='/pinjam' class='row'>");

        h.append("<select name='anggota'>");
        for (Anggota a : service.semuaAnggota()) {
            h.append("<option value='").append(esc(a.getId())).append("'>");
            h.append(esc(a.getNama())).append("</option>");
        }
        h.append("</select>");

        h.append("<select name='item'>");
        for (Item item : service.semuaItem()) {
            if (item.isTersedia()) {
                h.append("<option value='").append(esc(item.getId())).append("'>");
                h.append(esc(item.getJudul())).append("</option>");
            }
        }
        h.append("</select>");
        h.append("<input type='date' name='tanggal' title='Kosong = hari ini'>");
        h.append("<button>Pinjam</button></form>");

        h.append("<h3>Pinjaman aktif</h3>");
        h.append("<table><tr><th>Transaksi</th><th>Anggota</th><th>Koleksi</th>");
        h.append("<th>Jatuh tempo</th><th>Kembalikan</th></tr>");
        List<Peminjaman> aktif = service.pinjamanAktif();
        for (Peminjaman p : aktif) {
            h.append("<tr><td>").append(p.getId()).append("</td>");
            h.append("<td>").append(esc(p.getAnggota().getNama())).append("</td>");
            h.append("<td>").append(esc(p.getItem().getJudul())).append("</td>");
            h.append("<td>").append(p.getJatuhTempo()).append("</td>");
            h.append("<td><form method='post' action='/kembali' class='row'>");
            h.append("<input type='hidden' name='trx' value='").append(p.getId()).append("'>");
            h.append("<input type='date' name='tanggal' title='Kosong = hari ini'>");
            h.append("<button>Kembalikan</button></form></td></tr>");
        }
        if (aktif.isEmpty()) {
            h.append("<tr><td colspan='5'>Tidak ada pinjaman aktif</td></tr>");
        }
        h.append("</table></section>");
        return h.toString();
    }

    private String bagianRiwayat() {
        StringBuilder h = new StringBuilder();
        h.append("<section><h2>Riwayat</h2>");
        h.append("<table><tr><th>Transaksi</th><th>Anggota</th><th>Koleksi</th>");
        h.append("<th>Pinjam</th><th>Kembali</th><th>Denda</th></tr>");
        List<Peminjaman> semua = service.riwayat();
        for (Peminjaman p : semua) {
            String tglKembali = "-";
            String denda = "-";
            if (!p.isAktif()) {
                tglKembali = p.getTglKembali().toString();
                denda = rupiah(p.getDenda());
            }
            h.append("<tr><td>").append(p.getId()).append("</td>");
            h.append("<td>").append(esc(p.getAnggota().getNama())).append("</td>");
            h.append("<td>").append(esc(p.getItem().getJudul())).append("</td>");
            h.append("<td>").append(p.getTglPinjam()).append("</td>");
            h.append("<td>").append(tglKembali).append("</td>");
            h.append("<td>").append(denda).append("</td></tr>");
        }
        if (semua.isEmpty()) {
            h.append("<tr><td colspan='6'>Belum ada transaksi</td></tr>");
        }
        h.append("</table></section>");
        return h.toString();
    }

    private static String tombolHapus(String id) {
        return "<form method='post' action='/item/hapus'>"
                + "<input type='hidden' name='id' value='" + esc(id) + "'>"
                + "<button class='link'>Hapus</button></form>";
    }

    private static String rupiah(double nilai) {
        return "Rp" + String.format("%,.0f", nilai).replace(',', '.');
    }

    private static String enc(String teks) {
        return URLEncoder.encode(teks, StandardCharsets.UTF_8);
    }

    // ---------- Mengirim respons ----------

    private static void send(HttpExchange ex, int status, String isi) throws IOException {
        byte[] bytes = isi.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        OutputStream out = ex.getResponseBody();
        out.write(bytes);
        out.close();
    }

    /** Mengalihkan browser ke halaman utama (kode 303) sambil membawa pesan hasil. */
    private static void redirect(HttpExchange ex, String query) throws IOException {
        ex.getResponseHeaders().set("Location", "/?" + query);
        ex.sendResponseHeaders(303, -1);
        ex.close();
    }

    // ---------- Potongan HTML tetap ----------

    private static final String HEAD = "<!DOCTYPE html><html lang='id'><head><meta charset='utf-8'>"
            + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
            + "<title>Perpustakaan Mini</title><style>"
            + "body{font-family:system-ui,sans-serif;max-width:960px;margin:2rem auto;padding:0 1rem;color:#1a1a1a}"
            + "h1{margin-bottom:.5rem}"
            + "section{margin:1.5rem 0;padding:1rem;border:1px solid #ddd;border-radius:8px}"
            + "table{width:100%;border-collapse:collapse;margin:.5rem 0}"
            + "th,td{text-align:left;padding:.4rem;border-bottom:1px solid #eee;font-size:.9rem}"
            + ".row{display:flex;gap:.5rem;flex-wrap:wrap;align-items:center;margin:.5rem 0}"
            + "input,select,button{padding:.4rem .6rem;font:inherit} button{cursor:pointer}"
            + "button.link{background:none;border:none;color:#b00020;padding:0}"
            + ".ok{background:#e6f4ea;padding:.6rem;border-radius:6px}"
            + ".err{background:#fde8e8;padding:.6rem;border-radius:6px}"
            + "</style></head><body>";

    private static final String FORM_ITEM = "<form method='post' action='/item/tambah' class='row'>"
            + "<select name='jenis'><option value='buku'>Buku</option>"
            + "<option value='majalah'>Majalah</option><option value='jurnal'>Jurnal</option></select>"
            + "<input name='id' placeholder='ID' required size='6'>"
            + "<input name='judul' placeholder='Judul' required>"
            + "<input name='tahun' type='number' placeholder='Tahun' required style='width:6rem'>"
            + "<input name='extra' placeholder='Penulis / Edisi / ISSN' required>"
            + "<button>Tambah koleksi</button></form>";

    private static final String FORM_ANGGOTA = "<form method='post' action='/anggota/tambah' class='row'>"
            + "<select name='jenis'><option value='mahasiswa'>Mahasiswa</option>"
            + "<option value='dosen'>Dosen</option></select>"
            + "<input name='id' placeholder='ID' required size='6'>"
            + "<input name='nama' placeholder='Nama' required>"
            + "<button>Tambah anggota</button></form>";
}
