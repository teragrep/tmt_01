/*
 * Time-Partitioned Monoid Tree for Teragrep (tmt_01)
 * Copyright (C) 2026 Suomen Kanuuna Oy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 * Additional permission under GNU Affero General Public License version 3
 * section 7
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with other code, such other code is not for that reason alone subject to any
 * of the requirements of the GNU Affero GPL version 3 as long as this Program
 * is the same Program as licensed from Suomen Kanuuna Oy without any additional
 * modifications.
 *
 * Supplemented terms under GNU Affero General Public License version 3
 * section 7
 *
 * Origin of the software must be attributed to Suomen Kanuuna Oy. Any modified
 * versions must be marked as "Modified version of" The Program.
 *
 * Names of the licensors and authors may not be used for publicity purposes.
 *
 * No rights are granted for use of trade names, trademarks, or service marks
 * which are in The Program if any.
 *
 * Licensee must indemnify licensors and authors for any liability that these
 * contractual assumptions impose on licensors and authors.
 *
 * To the extent this program is licensed as part of the Commercial versions of
 * Teragrep, the applicable Commercial License may apply to this file if you as
 * a licensee so wish it.
 */
package com.teragrep.tmt_01;

import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.goterl.lazysodium.interfaces.Ristretto255;
import com.teragrep.tmt_01.node.Root;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class ReadmeTest {

    @Test
    void main() throws NoSuchAlgorithmException {
        // initialize first lazySodiumJava so we use only one instance
        LazySodiumJava lazySodiumJava = new LazySodiumJava(new SodiumJava());

        // our example payload to be ingested into the tree
        RistrettoPoint pointDelta = bytesToPoint(lazySodiumJava, "Hello World!".getBytes(StandardCharsets.UTF_8));

        Change change = new ChangeImpl(1L, Instant.EPOCH, pointDelta);

        // identityElement that sums up to itself
        RistrettoPoint identityElement = new LazysodiumRistrettoPoint(
                lazySodiumJava,
                new byte[Ristretto255.RISTRETTO255_BYTES]
        );

        // tree initialization
        TimePartitionedMonoidTree tree = new TimePartitionedMonoidTreeImpl(identityElement);

        // cause tree to process the change producing a new root, processChange returns a new root
        Root newRoot = tree.processChange(change);

        // new tree can be forked from an existing just as easily
        TimePartitionedMonoidTree otherTree = new TimePartitionedMonoidTreeImpl(
                identityElement,
                new AtomicReference<>(newRoot)
        );

        otherTree.processChange(change);

        // ... a forest?

        // one can search delta between the trees by point() methods
        RistrettoPoint hourPoint = tree.root().year(1970).month(0).day(0).hour(0).point();
    }

    private static RistrettoPoint bytesToPoint(LazySodiumJava lazySodiumJava, byte[] data)
            throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        byte[] hash64 = digest.digest(data);
        byte[] point = new byte[Ristretto255.RISTRETTO255_BYTES];
        boolean success = lazySodiumJava.cryptoCoreRistretto255FromHash(point, hash64);
        if (!success) {
            throw new RuntimeException("Elligator 2 mapping failed");
        }
        return new LazysodiumRistrettoPoint(lazySodiumJava, point);
    }

}
