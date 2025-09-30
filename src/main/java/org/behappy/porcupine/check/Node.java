package org.behappy.porcupine.check;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class Node<T> {
    T value;
    /// call if match is nil, otherwise return
    Node<T> match;
    int id;
    Node<T> next;
    Node<T> prev;

    Node(T value, Node<T> match, int id) {
        this.value = value;
        this.match = match;
        this.id = id;
    }

    Node<T> insertBefore(Node<T> mark) {
        if (mark != null) {
            var beforeMark = mark.prev;
            mark.prev = this;
            this.next = mark;
            if (beforeMark != null) {
                this.prev = beforeMark;
                beforeMark.next = this;
            }
        }
        return this;
    }

    int length() {
        var l = 0;
        var n = this;
        while (n != null) {
            n = n.next;
            l++;
        }
        return l;
    }

    void lift() {
        this.prev.next = this.next;
        this.next.prev = this.prev;
        var match = this.match;
        match.prev.next = match.next;
        if (match.next != null) {
            match.next.prev = match.prev;
        }
    }

    void unlift() {
        var match = this.match;
        match.prev.next = match;
        if (match.next != null) {
            match.next.prev = match;
        }
        this.prev.next = this;
        this.next.prev = this;
    }
}
