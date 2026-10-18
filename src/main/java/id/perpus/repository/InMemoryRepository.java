package id.perpus.repository;

import id.perpus.model.Identifiable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryRepository<T extends Identifiable> implements Repository<T> {
    private final Map<String, T> data = new LinkedHashMap<>();

    @Override
    public void simpan(T entitas) { data.put(entitas.getId().toUpperCase(), entitas); }

    @Override
    public Optional<T> cariById(String id) { return Optional.ofNullable(data.get(id.trim().toUpperCase())); }

    @Override
    public List<T> semua() { return new ArrayList<>(data.values()); }

    @Override
    public boolean hapus(String id) { return data.remove(id.trim().toUpperCase()) != null; }
}
