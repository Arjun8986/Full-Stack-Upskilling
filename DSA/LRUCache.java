import java.util.HashMap;
import java.util.Map;

public class LRUCache {

    /** I'll use a HashMap for O(1) lookup and a Doubly Linked List to maintain the
     *  usage order. The head represents the most recently used item and the tail 
     * represents the least recently used item. On every get or update, I move the
     *  node to the head. When capacity is exceeded, I remove the node from the tail.
     */

    /*
     * Represents one item in the cache.
     *
     * Each Node contains:
     * key   -> the cache key
     * value -> the actual cached value
     * prev  -> previous node in the linked list
     * next  -> next node in the linked list
     */
    private class Node {

        int key;
        int value;

        Node prev;
        Node next;

        Node(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    /*
     * Maximum number of items the cache can store.
     *
     * Example:
     * capacity = 3
     *
     * The cache can contain only 3 items.
     */
    private final int capacity;

    /*
     * HashMap stores:
     *
     * key -> Node
     *
     * Example:
     *
     * 1 -> Node(1, 100)
     * 2 -> Node(2, 200)
     * 3 -> Node(3, 300)
     *
     * HashMap allows us to find a node in O(1).
     */
    private final Map<Integer, Node> cache;


    /*
     * Dummy head and tail nodes.
     *
     * head -> Most Recently Used side
     * tail -> Least Recently Used side
     *
     * We don't store actual cache data in these nodes.
     *
     * They make insertion and deletion easier.
     */
    private final Node head;
    private final Node tail;


    /*
     * Constructor
     */
    public LRUCache(int capacity) {

        // Store the maximum cache size
        this.capacity = capacity;

        // Create an empty HashMap
        this.cache = new HashMap<>();

        /*
         * Create dummy head and tail nodes.
         *
         * These are not actual cache items.
         */
        head = new Node(0, 0);
        tail = new Node(0, 0);

        /*
         * Initially the list looks like:
         *
         * head <-> tail
         */
        head.next = tail;
        tail.prev = head;
    }


    /*
     * Get a value from the cache.
     */
    public int get(int key) {

        /*
         * Check whether the key exists.
         *
         * If it doesn't exist, return -1.
         */
        if (!cache.containsKey(key)) {
            return -1;
        }

        /*
         * Get the Node from HashMap.
         *
         * HashMap gives us the Node in O(1).
         */
        Node node = cache.get(key);


        /*
         * This node has now been accessed.
         *
         * Therefore, it becomes the
         * MOST RECENTLY USED item.
         *
         * First remove it from its current position.
         */
        remove(node);

        /*
         * Then put it at the front of the list.
         */
        addToFront(node);


        /*
         * Return the cached value.
         */
        return node.value;
    }


    /*
     * Add a new key/value to the cache.
     */
    public void put(int key, int value) {

        /*
         * Check whether this key already exists.
         */
        if (cache.containsKey(key)) {

            /*
             * Get the existing Node.
             */
            Node node = cache.get(key);

            /*
             * Update its value.
             */
            node.value = value;

            /*
             * Since this key was just used,
             * remove it from its old position.
             */
            remove(node);

            /*
             * Move it to the front.
             *
             * Front = Most Recently Used
             */
            addToFront(node);

            /*
             * We are done.
             */
            return;
        }


        /*
         * Key does not exist.
         *
         * Create a new Node.
         */
        Node node = new Node(key, value);


        /*
         * Store the Node in the HashMap.
         *
         * key -> Node
         */
        cache.put(key, node);


        /*
         * New item is the most recently used item.
         *
         * Therefore, add it to the front.
         */
        addToFront(node);


        /*
         * Check whether the cache has exceeded
         * its maximum capacity.
         */
        if (cache.size() > capacity) {

            /*
             * The node just before tail is the
             * LEAST RECENTLY USED node.
             *
             * Example:
             *
             * head <-> 4 <-> 1 <-> 3 <-> tail
             *                       ↑
             *                   LRU item
             */
            Node leastRecentlyUsed = tail.prev;


            /*
             * Remove the LRU node from
             * the linked list.
             */
            remove(leastRecentlyUsed);


            /*
             * Also remove it from the HashMap.
             *
             * Both data structures must stay synchronized.
             */
            cache.remove(leastRecentlyUsed.key);
        }
    }


    /*
     * Add a node immediately after head.
     *
     * This means the node becomes
     * the MOST RECENTLY USED item.
     */
    private void addToFront(Node node) {

        /*
         * Current structure:
         *
         * head <-> oldFirst
         *
         * We want:
         *
         * head <-> node <-> oldFirst
         */

        // New node points to the current first node
        node.next = head.next;

        // New node points back to head
        node.prev = head;

        // Current first node points back to new node
        head.next.prev = node;

        // Head now points to the new node
        head.next = node;
    }


    /*
     * Remove a node from the doubly linked list.
     */
    private void remove(Node node) {

        /*
         * Example:
         *
         * A <-> Node <-> B
         *
         * We want:
         *
         * A <-> B
         */

        // Connect previous node to next node
        node.prev.next = node.next;

        // Connect next node to previous node
        node.next.prev = node.prev;
    }


     public static void main(String[] args) {

        LRUCache cache = new LRUCache(3);

        cache.put(1, 100);
        cache.put(2, 200);
        cache.put(3, 300);

        System.out.println(cache.get(1)); // 100

        cache.put(4, 400);

        System.out.println(cache.get(2)); // -1
        System.out.println(cache.get(3)); // 300
        System.out.println(cache.get(4)); // 400
    }


}