package host.minestudio.frost.api.shards.helper

import java.util.concurrent.CompletableFuture

interface StorageService {
    /**
     * Saves data to the storage service.
     *
     * @param storeName The name of the store where the data will be saved.
     * @param key The key under which the data will be stored.
     * @param value The data to be stored.
     */
    fun saveData(storeName: String, key: String, value: Any)

    /**
     * Retrieves data from the storage service.
     *
     * @param storeName The name of the store from which the data will be retrieved.
     * @param key The key under which the data is stored.
     * @return The data associated with the key, or null if not found.
     */
    fun getData(storeName: String, key: String): CompletableFuture<Any?>

    /**
     * Retrieves data from the storage service synchronously.
     *
     * @param storeName The name of the store from which the data will be retrieved.
     * @param key The key under which the data is stored.
     * @return The data associated with the key, or null if not found.
     */
    fun getDataSync(storeName: String, key: String): Any?

    /**
     * Deletes data from the storage service.
     *
     * @param storeName The name of the store from which the data will be deleted.
     * @param key The key under which the data is stored.
     */
    fun deleteData(storeName: String, key: String)
}