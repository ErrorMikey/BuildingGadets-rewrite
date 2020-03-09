package com.direwolf20.core.inventory;

import java.util.List;

public interface IItemIndex extends IItemCache {
    enum BindingResult {
        NO_BIND,
        REPLACE,
        BIND
    }

    IBulkItemTransaction bulkTransaction();

    @Override
    default int extractItem(IndexKey key, int count, boolean simulate) {
        IBulkItemTransaction transaction = bulkTransaction();
        count = transaction.extractItem(key, count, simulate);
        transaction.commit();
        return count;
    }

    @Override
    default int insertItem(IndexKey key, int count, boolean simulate) {
        IBulkItemTransaction transaction = bulkTransaction();
        count = transaction.insertItem(key, count, simulate);
        transaction.commit();
        return count;
    }

    /**
     * Calling this Method will ensure that the index is accurate. Any sub-sequent extract and insert calls will reflect exactly
     * the environments state (until the env. is changed of course). Notice that calling this Method may result in a high computational cost!
     * <p>
     * Use this Method in favor of {@link #updateIndex()}, if you need the index to be accurate right now.
     *
     * @return Whether or not the index is up-to-date after this Method call, or not.
     */
    boolean reIndex();

    /**
     * Perform some update action on the index. This is not guaranteed to provide a fully accurate index, nor is it
     * guaranteed to perform any update at all. How much is updated (if at all), is entirely up to the implementation.
     * It is recommended that an implementation ensures however, that repeated calls to this Method (how many is unspecified)
     * have the same result as {@link #reIndex()}
     * <p>
     * Use this Method for in favor of {@link #reIndex()}, if you don't need the index to be accurate and can live with it catching a change
     * only after a few calls to this Methods (for example some ticks later).
     *
     * @return Whether anything was updated or not
     */
    boolean updateIndex();

    BindingResult bind(InventoryLink other);

    boolean unbind(InventoryLink other);

    List<InventoryLink> boundLinks();
}
