package id.perpus.repository;

import id.perpus.model.Identifiable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Penyimpanan di memori. Kunci map adalah ID dalam huruf besar (b001 sama dengan B001). */
public class InMemoryRepository<T extends Identifiable> implements Repository<T> {
    private final Map<String, T> data = new LinkedHashMap<>();

    @Override
    public void simpan(T entitas) {
        data.put(entitas.getId().toUpperCase(), entitas);
    }

    @Override
    public T cariById(String id) {
        return data.get(id.trim().toUpperCase());
    }

    @Override
    public List<T> semua() {
        // Salinan, supaya pemanggil tidak bisa mengubah isi map asli
        return new ArrayList<>(data.values());
    }

    @Override
    public boolean hapus(String id) {
        return data.remove(id.trim().toUpperCase()) != null;
    }
}
