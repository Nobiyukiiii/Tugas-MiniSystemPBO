package id.perpus.repository;

import id.perpus.model.Identifiable;
import java.util.List;
import java.util.Optional;

public interface Repository<T extends Identifiable> {
    void simpan(T entitas);

    Optional<T> cariById(String id);

    List<T> semua();

    boolean hapus(String id);
}
