package id.perpus.web;

import static id.perpus.web.Html.esc;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import id.perpus.model.*;
import id.perpus.repository.InMemoryRepository;
import id.perpus.service.PerpustakaanException;
import id.perpus.service.PerpustakaanService;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/** Server web sederhana (JDK HttpServer). Semua logika bisnis ada di PerpustakaanService. */
public class WebApp {
    private final PerpustakaanService service =
            new PerpustakaanService(new InMemoryRepository<>(), new InMemoryRepository<>());

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        WebApp app = new WebApp();
        app.seed();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", app::handle); // executor bawaan: request diproses satu per satu
        server.start();
        System.out.println("Perpustakaan berjalan di http://localhost:" + port + " (Ctrl+C untuk berhenti)");
    }

    private void seed() {
        service.tambahItem(new Buku("B001", "Clean Code", 2008, "Robert C. Martin"));
        service.tambahItem(new Buku("B002", "Laskar Pelangi", 2005, "Andrea Hirata"));
        service.tambahItem(new Majalah("M001", "Tempo Edisi Khusus", 2023, "No. 12"));
        service.tambahItem(new Jurnal("J001", "Jurnal Informatika", 2022, "1978-1234"));
        service.tambahAnggota(new Mahasiswa("A001", "Budi Santoso"));
        service.tambahAnggota(new Dosen("D001", "Hardiansyah"));
    }

    // ---------- Routing ----------

    private void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if (method.equals("GET") && path.equals("/")) {
                send(ex, 200, halaman(Html.parse(ex.getRequestURI().getRawQuery())));
            } else if (method.equals("POST")) {
                Map<String, String> form = Html.parse(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                redirect(ex, aksi(path, form));
            } else {
                send(ex, 404, "Halaman tidak ditemukan");
            }
        } catch (RuntimeException e) {
            send(ex, 500, "Terjadi kesalahan server");
            e.printStackTrace();
        }
    }

    /** Menjalankan aksi POST dan mengembalikan query string untuk pesan hasil. */
    private String aksi(String path, Map<String, String> f) {
        try {
            switch (path) {
                case "/item/tambah" -> service.tambahItem(buatItem(f));
                case "/item/hapus" -> service.hapusItem(f.get("id"));
                case "/anggota/tambah" -> service.tambahAnggota(buatAnggota(f));
                case "/pinjam" -> service.pinjam(f.get("anggota"), f.get("item"), tanggal(f.get("tanggal")));
                case "/kembali" -> {
                    Peminjaman p = service.kembali(f.get("trx"), tanggal(f.get("tanggal")));
                    return "ok=" + enc("Dikembalikan. Denda: " + rupiah(p.getDenda()));
                }
                default -> throw new PerpustakaanException("Aksi tidak dikenal");
            }
            return "ok=" + enc("Berhasil");
        } catch (PerpustakaanException | IllegalArgumentException | DateTimeParseException | NullPointerException e) {
            String pesan = e instanceof NullPointerException ? "Data form tidak lengkap" : e.getMessage();
            return "err=" + enc(pesan);
        }
    }

    private Item buatItem(Map<String, String> f) {
        String id = f.get("id"), judul = f.get("judul");
        int tahun = Integer.parseInt(f.getOrDefault("tahun", ""));
        String extra = f.get("extra");
        return switch (f.getOrDefault("jenis", "")) {
            case "buku" -> new Buku(id, judul, tahun, extra);
            case "majalah" -> new Majalah(id, judul, tahun, extra);
            case "jurnal" -> new Jurnal(id, judul, tahun, extra);
            default -> throw new IllegalArgumentException("Jenis koleksi tidak valid");
        };
    }

    private Anggota buatAnggota(Map<String, String> f) {
        return switch (f.getOrDefault("jenis", "")) {
            case "mahasiswa" -> new Mahasiswa(f.get("id"), f.get("nama"));
            case "dosen" -> new Dosen(f.get("id"), f.get("nama"));
            default -> throw new IllegalArgumentException("Jenis anggota tidak valid");
        };
    }

    private static LocalDate tanggal(String s) {
        return s == null || s.isBlank() ? LocalDate.now() : LocalDate.parse(s);
    }

    // ---------- Tampilan ----------

    private String halaman(Map<String, String> q) {
        String cari = q.getOrDefault("q", "");
        List<Item> items = cari.isBlank() ? service.semuaItem() : service.cariItemByJudul(cari);
        StringBuilder h = new StringBuilder(HEAD);
        h.append("<h1>Perpustakaan Mini</h1>");
        if (q.containsKey("ok")) h.append("<p class='ok'>").append(esc(q.get("ok"))).append("</p>");
        if (q.containsKey("err")) h.append("<p class='err'>").append(esc(q.get("err"))).append("</p>");

        h.append("<section><h2>Koleksi</h2><form method='get' class='row'>")
                .append("<input name='q' placeholder='Cari judul' value='").append(esc(cari)).append("'>")
                .append("<button>Cari</button></form>");
        h.append("<table><tr><th>ID</th><th>Jenis</th><th>Judul</th><th>Tahun</th><th>Detail</th><th>Status</th><th></th></tr>");
        for (Item i : items) {
            h.append("<tr><td>").append(esc(i.getId())).append("</td><td>").append(i.getJenis())
                    .append("</td><td>").append(esc(i.getJudul())).append("</td><td>").append(i.getTahun())
                    .append("</td><td>").append(esc(i.getDetail())).append("</td><td>")
                    .append(i.isTersedia() ? "Tersedia" : "Dipinjam").append("</td><td>")
                    .append(postButton("/item/hapus", "id", i.getId(), "Hapus")).append("</td></tr>");
        }
        if (items.isEmpty()) h.append("<tr><td colspan='7'>Tidak ada koleksi</td></tr>");
        h.append("</table>").append(FORM_ITEM).append("</section>");

        h.append("<section><h2>Anggota</h2><table><tr><th>ID</th><th>Jenis</th><th>Nama</th><th>Pinjaman</th></tr>");
        for (Anggota a : service.semuaAnggota()) {
            h.append("<tr><td>").append(esc(a.getId())).append("</td><td>").append(a.getJenis())
                    .append("</td><td>").append(esc(a.getNama())).append("</td><td>")
                    .append(a.getPinjamanAktif().size()).append("/").append(a.getMaxPinjam()).append("</td></tr>");
        }
        h.append("</table>").append(FORM_ANGGOTA).append("</section>");

        h.append("<section><h2>Peminjaman</h2><form method='post' action='/pinjam' class='row'><select name='anggota'>");
        for (Anggota a : service.semuaAnggota())
            h.append("<option value='").append(esc(a.getId())).append("'>").append(esc(a.getNama())).append("</option>");
        h.append("</select><select name='item'>");
        for (Item i : service.semuaItem())
            if (i.isTersedia())
                h.append("<option value='").append(esc(i.getId())).append("'>").append(esc(i.getJudul())).append("</option>");
        h.append("</select><input type='date' name='tanggal' title='Kosong = hari ini'><button>Pinjam</button></form>");

        h.append("<h3>Pinjaman aktif</h3><table><tr><th>Transaksi</th><th>Anggota</th><th>Koleksi</th><th>Jatuh tempo</th><th>Kembalikan</th></tr>");
        for (Peminjaman p : service.pinjamanAktif()) {
            h.append("<tr><td>").append(p.getId()).append("</td><td>").append(esc(p.getAnggota().getNama()))
                    .append("</td><td>").append(esc(p.getItem().getJudul())).append("</td><td>").append(p.getJatuhTempo())
                    .append("</td><td><form method='post' action='/kembali' class='row'><input type='hidden' name='trx' value='")
                    .append(p.getId()).append("'><input type='date' name='tanggal' title='Kosong = hari ini'>")
                    .append("<button>Kembalikan</button></form></td></tr>");
        }
        if (service.pinjamanAktif().isEmpty()) h.append("<tr><td colspan='5'>Tidak ada pinjaman aktif</td></tr>");
        h.append("</table></section>");

        h.append("<section><h2>Riwayat</h2><table><tr><th>Transaksi</th><th>Anggota</th><th>Koleksi</th><th>Pinjam</th><th>Kembali</th><th>Denda</th></tr>");
        for (Peminjaman p : service.riwayat()) {
            h.append("<tr><td>").append(p.getId()).append("</td><td>").append(esc(p.getAnggota().getNama()))
                    .append("</td><td>").append(esc(p.getItem().getJudul())).append("</td><td>").append(p.getTglPinjam())
                    .append("</td><td>").append(p.isAktif() ? "-" : p.getTglKembali()).append("</td><td>")
                    .append(p.isAktif() ? "-" : rupiah(p.getDenda())).append("</td></tr>");
        }
        if (service.riwayat().isEmpty()) h.append("<tr><td colspan='6'>Belum ada transaksi</td></tr>");
        return h.append("</table></section></body></html>").toString();
    }

    private static String postButton(String action, String name, String value, String label) {
        return "<form method='post' action='" + action + "'><input type='hidden' name='" + name + "' value='"
                + esc(value) + "'><button class='link'>" + label + "</button></form>";
    }

    private static String rupiah(double v) { return String.format("Rp%,.0f", v).replace(',', '.'); }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    // ---------- HTTP helper ----------

    private static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (var out = ex.getResponseBody()) { out.write(bytes); }
    }

    private static void redirect(HttpExchange ex, String query) throws IOException {
        ex.getResponseHeaders().set("Location", "/?" + query);
        ex.sendResponseHeaders(303, -1);
        ex.close();
    }

    private static final String HEAD = """
            <!DOCTYPE html><html lang="id"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>Perpustakaan Mini</title><style>
            body{font-family:system-ui,sans-serif;max-width:960px;margin:2rem auto;padding:0 1rem;color:#1a1a1a}
            h1{margin-bottom:.5rem} section{margin:1.5rem 0;padding:1rem;border:1px solid #ddd;border-radius:8px}
            table{width:100%;border-collapse:collapse;margin:.5rem 0} th,td{text-align:left;padding:.4rem;border-bottom:1px solid #eee;font-size:.9rem}
            .row{display:flex;gap:.5rem;flex-wrap:wrap;align-items:center;margin:.5rem 0}
            input,select,button{padding:.4rem .6rem;font:inherit} button{cursor:pointer}
            button.link{background:none;border:none;color:#b00020;padding:0}
            .ok{background:#e6f4ea;padding:.6rem;border-radius:6px} .err{background:#fde8e8;padding:.6rem;border-radius:6px}
            </style></head><body>""";

    private static final String FORM_ITEM = """
            <form method="post" action="/item/tambah" class="row">
            <select name="jenis"><option value="buku">Buku</option><option value="majalah">Majalah</option><option value="jurnal">Jurnal</option></select>
            <input name="id" placeholder="ID" required size="6"><input name="judul" placeholder="Judul" required>
            <input name="tahun" type="number" placeholder="Tahun" required style="width:6rem">
            <input name="extra" placeholder="Penulis / Edisi / ISSN" required><button>Tambah koleksi</button></form>""";

    private static final String FORM_ANGGOTA = """
            <form method="post" action="/anggota/tambah" class="row">
            <select name="jenis"><option value="mahasiswa">Mahasiswa</option><option value="dosen">Dosen</option></select>
            <input name="id" placeholder="ID" required size="6"><input name="nama" placeholder="Nama" required>
            <button>Tambah anggota</button></form>""";
}
