# NeoForge 全バージョン対応 実装計画

このファイルは別チャットへ作業を引き継ぐための計画メモです。このチャットでは実装しない。

## 目的

既存の Fabric 版各バージョンブランチに対して、NeoForge 版も同じ範囲で対応させる。

加えて、全バージョンで同じ機能セットと同じ `mod_version` に揃える。正とする最新仕様は「取引後に村人ごと・プレイヤーごとにリロールをロックする」仕様。

既に `neoforge/26.1` は作成済みで、`26.1` ブランチからマルチローダー構成へ移植されている。ただし現在の `neoforge/26.1` には取引済み永続ロック仕様が入っていないため、NeoForge 横展開の前に機能差を解消する必要がある。

## 現在確認できているブランチ

Fabric / 元ブランチ:

- `main` = Minecraft `1.21.11`
- `1.21`
- `1.21.2`
- `1.21.4`
- `1.21.5`
- `1.21.6`
- `1.21.9`
- `26.1`

NeoForge 対応済み:

- `neoforge/26.1`

機能差の現状:

- `1.21`, `1.21.2`, `1.21.4`, `1.21.5`, `1.21.6`, `1.21.9`: 取引済み永続ロックあり
- `main` = `1.21.11`: 取引済み永続ロックなし
- `26.1`: 取引済み永続ロックなし
- `neoforge/26.1`: 取引済み永続ロックなし

最初に揃えるべき既存ブランチ:

- `main`
- `26.1`
- `neoforge/26.1`

作成予定:

- `neoforge/1.21.11` from `main`
- `neoforge/1.21.9` from `1.21.9`
- `neoforge/1.21.6` from `1.21.6`
- `neoforge/1.21.5` from `1.21.5`
- `neoforge/1.21.4` from `1.21.4`
- `neoforge/1.21.2` from `1.21.2`
- `neoforge/1.21` from `1.21`

## 既存 `neoforge/26.1` の主な変更内容

`26.1..neoforge/26.1` の差分では、単なる NeoForge ファイル追加ではなく、プロジェクト構造をマルチローダー化している。

主な変更:

- ルート `build.gradle` を共通設定中心に変更
- `settings.gradle` で `fabric` / `neoforge` を include
- 共通コードを `common/src/main/...` に移動
- Fabric 専用コードを `fabric/src/main/...` に追加
- NeoForge 専用コードを `neoforge/src/main/...` に追加
- `PlatformHooks` / `PlatformServices` で loader 差分を吸収
- `fabric.mod.json` と `neoforge.mods.toml` を loader ごとに分離
- networking / keybind / config dir / particle send を platform hooks 経由に分離
- GitHub Actions の build 対象をマルチプロジェクト向けに変更

注意:

- `common/build.gradle` は存在するが、現在の `settings.gradle` では `common` は include されていない。
- 実際には `fabric` / `neoforge` の `sourceSets` が `../common/src/main/java` と `../common/src/main/resources` を直接読む構成。
- 今後の移植時も、この構成を維持するか、`common` を正式サブプロジェクト化するかを最初に決める。

## 推奨作業順

まず仕様と方針を固定し、次に既存 Fabric 側の機能差を揃え、その後 NeoForge へ横展開する。API 差分が小さい順に進めることで、失敗したときの原因を切り分けやすくする。

Phase 0: 実装前確認

1. `1.21.9` をロック仕様の正として読む
2. 全ブランチの `mod_version` を `3.0.0` に揃える
3. NeoForge の永続ロック保存方式を確認する
4. 新構成へ移行したブランチで旧 `src/main` を削除するか決める
5. loader ごとの artifact 名と CI 方針を決める

Phase 1: Fabric 側の最新仕様化

1. `main` で Fabric 側コードを整理しつつ取引済み永続ロック仕様を入れる
2. `main` の build / client 起動 / dedicated server 起動を確認する
3. `26.1` で同じ仕様を 26.1 API / Mojang mappings / Java 25 に合わせて入れる
4. `26.1` の build / client 起動 / dedicated server 起動を確認する

Phase 2: 既存 NeoForge ブランチの最新仕様化

1. `neoforge/26.1` に取引済み永続ロック仕様を入れる
2. Fabric と NeoForge の loader 境界をそろえる
3. `:fabric:build` / `:neoforge:build` を確認する
4. NeoForge client / dedicated server を確認する

Phase 3: 共通 metadata / artifact / CI 調整

1. 全対象ブランチの `mod_version` を `3.0.0` に揃える
2. Fabric / NeoForge の jar 名を loader name + mod version + Minecraft version の順に揃える
3. GitHub Actions が両 loader の artifact を拾うようにする
4. metadata と説明文を loader / Minecraft version ごとに確認する

Phase 4: 追加 NeoForge ブランチ作成

1. `neoforge/1.21.11` from `main`
2. `neoforge/1.21.9` from `1.21.9`
3. `neoforge/1.21.6` from `1.21.6`
4. `neoforge/1.21.5` from `1.21.5`
5. `neoforge/1.21.4` from `1.21.4`
6. `neoforge/1.21.2` from `1.21.2`
7. `neoforge/1.21` from `1.21`

Phase 5: 最終確認

1. 全 branch が build できる
2. 全 branch で取引済み永続ロック仕様が同じ
3. 全 branch の `mod_version` が `3.0.0`
4. 全 branch で Fabric / NeoForge の成果物名が loader name + mod version + Minecraft version の順になっている
5. 全 branch を origin に push する

## 統一する最新仕様

全バージョンで揃える機能:

- villager ごと、player ごとに取引済みロックを保持する
- 取引が成立したら、その player UUID を対象 villager に保存する
- 保存された player は、その villager の trade を再リロールできない
- ロックは server restart 後も残る
- ロック済みの場合、クライアントのリロールボタンを無効化する
- `requireSneaking` が true でスニークしていない場合は拒否し、ボタンは再度押せる状態に戻す
- nitwit / unemployed など trade table がない場合は拒否し、ボタンは再度押せる状態に戻す
- リロール成功時は trade list を画面を閉じずに同期し、particle を出す
- Fabric と NeoForge で config file はそれぞれの config dir に保存する

必須要素:

- C2S: reroll request payload
- S2C: particle payload
- S2C: locked payload
- S2C: reject payload
- client 側: 楽観的にボタンを無効化し、reject で戻す
- server 側: 取引成立時に villager へ player UUID を保存する

loader ごとの保存方式:

- Fabric: Fabric Data Attachment を使う
- NeoForge: NeoForge で server restart 後も残る attachment / persistent data 相当を使う

注意:

- NeoForge 側では Fabric の `AttachmentRegistry` を使えない。NeoForge で同等の永続データ保存方法を選ぶ。
- 保存データの namespace は `reroll-trades:locked_players` 相当の意味を保つ。
- `IRerollLockable` は `net.misemise` package に置く。
- `MerchantScreenMixin` は `IRerollLockable` を実装し、client packet handler から `currentScreen instanceof IRerollLockable` で呼ぶ。

## Fabric 側コード整理方針

NeoForge 対応を安定させるには、Fabric 側コードの整理も必要。目的はリファクタ自体ではなく、同じ機能を Fabric / NeoForge で共有できる境界を作ること。

整理の原則:

- `common` には loader 非依存の仕様だけを置く
- Fabric API import は `fabric/src/main/...` に閉じ込める
- NeoForge API import は `neoforge/src/main/...` に閉じ込める
- common 側の `RerollTrades` は `ModInitializer` や `@Mod` を実装しない
- Fabric entrypoint は `RerollTradesFabric`
- NeoForge entrypoint は `RerollTradesNeoForge`
- networking 登録、keybind 登録、config dir、永続ロック保存は loader ごとの実装に分ける

Fabric 側から common へ移す候補:

- reroll 可否判定
- requireSneaking 判定
- trade list 再生成方針
- client へ送る結果の意味
- particle 表示処理の共通部分
- config model

Fabric 側に残すもの:

- `PayloadTypeRegistry`
- `ServerPlayNetworking`
- `ClientPlayNetworking`
- `KeyBindingHelper`
- `FabricLoader.getConfigDir()`
- `AttachmentRegistry` / `AttachmentType`
- Fabric entrypoint

NeoForge 側に作るもの:

- `RegisterPayloadHandlersEvent` / `PayloadRegistrar`
- `RegisterClientPayloadHandlersEvent`
- `RegisterKeyMappingsEvent`
- `ClientPacketDistributor` / `PacketDistributor`
- `FMLPaths.CONFIGDIR`
- NeoForge の永続データ保存実装
- NeoForge entrypoint

特に整理が必要な境界:

1. packet の定義と登録

   packet の意味は common、登録 API は loader 側。

   common:

   - `RerollTradesPayload`
   - `RerollParticlePayload`
   - `RerollLockedPayload`
   - `RerollRejectPayload`

   loader:

   - Fabric の payload registry / receiver
   - NeoForge の payload registrar / handler

2. lock state の保存

   common は「この player はこの villager に対して locked か」「取引後に lock する」という意味だけを扱う。

   loader 側:

   - Fabric: Data Attachment
   - NeoForge: NeoForge 側の永続データ保存

3. client screen の lock / unlock

   `MerchantScreenMixin` は common に置ける可能性があるが、key input API がバージョンで違うため、ブランチごとに注意する。

   - `IRerollLockable` は common package `net.misemise`
   - packet handler は loader 側
   - handler から `currentScreen instanceof IRerollLockable` で lock / unlock

4. keybind

   keybind 登録と key match は loader / Minecraft バージョン依存。

   common に `KeyEvent` など client-only 型を置くと dedicated server crash の原因になるので、client-only interface または loader client class に寄せる。

整理しないもの:

- gameplay 仕様を変える不要な改名
- unrelated な formatting
- 古いバージョンの API 差分を消すための一括置換
- `common` の正式 Gradle サブプロジェクト化。これは別判断にする。

おすすめ手順:

1. まず最新仕様を文章で固定する
2. 既存 Fabric ロック仕様を読み、common に移せる仕様部分と Fabric に残す API 部分を分ける
3. `main` で Fabric 側を整理しつつロック仕様を入れる
4. `26.1` で同じ機能を Mojang mapping / Java 25 / 26.1 API に合わせて入れる
5. `neoforge/26.1` で Fabric と NeoForge の loader 境界をそろえる
6. その形を古い NeoForge ブランチへ横展開する

## 追加で決める・確認すること

実装前にここを決めておくと、途中で方針がぶれにくい。

### 1. 正とする元ブランチ

ロック仕様の正は `1.21.9` 系を第一候補にする。

理由:

- `1.21` から `1.21.9` の中では比較的新しい
- `RerollLockedPayload` / `RerollRejectPayload` / `IRerollLockable` / Data Attachment の構成が揃っている
- `1.21.5+` の `VillagerData` record 系 API に対応済み

ただし `1.21.5` はローカルと `origin/1.21.5` に差があるため、ロック仕様の参照元にしない。必要な場合は `1.21.6` と照合する。

### 2. `mod_version`

全ブランチで `3.0.0` に揃える。

理由:

- 全バージョンで機能セットを揃える
- 取引済み永続ロック仕様を最新仕様として統一する
- Fabric / NeoForge のマルチローダー対応を進める
- 旧 `1.x` 系から見て配布単位と内部構成が大きく変わる

注意:

- `version = "${mod_version}+${minecraft_version}"` のブランチと `version = mod_version` のブランチがある。
- jar 名や metadata 上の version 表示をどうしたいかも合わせて確認する。

### 3. NeoForge の永続ロック保存方式

Fabric は Data Attachment を使っているが、NeoForge では別実装が必要。

実装時に確認すること:

- villager entity に player UUID set を server restart 後も保存できるか
- world save/load 後に復元されるか
- client には保存せず server authoritative にする
- Fabric と同じ意味になるか

候補:

- NeoForge の attachment / capability / persistent data 系 API
- entity NBT 相当の永続データ

ここは実装時に対象 Minecraft / NeoForge version ごとの API を確認してから決める。

### 3.5 NeoForge 対応バージョン表

実装前に、対象 Minecraft バージョンごとの NeoForge version / loader range / Java version を確認して表にする。

必要な項目:

- Minecraft version
- NeoForge version
- NeoForge loader version range
- ModDevGradle version
- Java version
- その Minecraft version で使える payload / attachment / persistent data API

注意:

- `26.1` は既に `neoforge_version=26.1.0.0-alpha.13+snapshot-10` を使っている。
- `1.21.x` 系の NeoForge version は実装時に公式 Maven / docs / 既存プロジェクト例で確認する。
- ここを推測で埋めない。

### 4. 旧 `src/main` の扱い

`neoforge/26.1` では旧 `src/main` が残っている。

決めること:

- 新構成へ移行したブランチでは旧 `src/main` を削除するか
- 残す場合、誤編集を防ぐためにどう扱うか

おすすめ:

- 新構成へ移行したブランチでは旧 `src/main` を削除する。
- 削除しない場合でも、build 対象外であることを計画と commit message に明記する。

### 5. metadata と説明文

各 loader / version で metadata がずれやすい。

確認するもの:

- `fabric.mod.json` の `name`, `description`, `depends.minecraft`, `depends.java`
- `neoforge.mods.toml` の `modId`, `versionRange`, `loaderVersion`, `displayName`
- `authors`, `license`, `icon`, `sources`, `issueTrackerURL`
- lang key の有無

注意:

- Fabric mod id は `reroll-trades`
- NeoForge mod id は `reroll_trades`
- assets namespace は `reroll-trades`

### 6. artifact / jar 名

全ブランチで loader name + mod version + Minecraft version が分かる成果物名にする。

命名順:

```text
<mod-name>-<loader-name>-<mod-version>-<minecraft-version>.jar
```

例:

- `reroll-trades-fabric-3.0.0-1.21.11.jar`
- `reroll-trades-neoforge-3.0.0-1.21.11.jar`
- `reroll-trades-fabric-3.0.0-26.1.jar`
- `reroll-trades-neoforge-3.0.0-26.1.jar`

実際の命名は Gradle 設定に合わせて決める。少なくとも Fabric と NeoForge の jar が同名で上書きされないこと。

### 7. CI

GitHub Actions はマルチプロジェクト化後に以下を確認する。

- Fabric jar が artifact に含まれる
- NeoForge jar が artifact に含まれる
- Java version が対象ブランチに合っている
- 古い Java 21 系ブランチに Java 25 を入れていない
- `./gradlew build` が root で両 loader を build する構成になっているか

### 8. 動作確認ワールド

手動確認用に同じ観点で見る。

最低限の確認ケース:

- 新規村人でリロールできる
- 取引前なら複数回リロールできる
- 取引後は同じ player + villager でリロール不可
- server restart 後も取引済みロックが残る
- 別 player は同じ villager をリロールできるか、仕様通り確認する
- 別 villager は同じ player でもリロールできる
- nitwit / unemployed で拒否され、ボタンが戻る
- `requireSneaking=true` で非スニーク拒否、スニーク時成功
- dedicated server で client-only class crash がない

### 9. config / lock data の互換性

`reroll-trades.json` は全 loader で同じ filename を維持する。

取引済みロックの保存データは新規追加扱いでよい。ただし:

- 既存ワールドに導入しても crash しない
- 保存データがない villager は未ロック扱い
- 保存データの読み取り失敗時に world を壊さない

### 10. 作業単位

おすすめの commit 単位:

1. `main` にロック仕様追加 + Fabric 整理
2. `26.1` にロック仕様追加 + Fabric 整理
3. `neoforge/26.1` にロック仕様追加
4. `mod_version` / artifact naming / CI 調整
5. 各 `neoforge/<version>` ブランチ作成

一度に全部やると失敗時の原因追跡が難しいので、1ブランチずつ build して commit する。

## 26.1 と 1.21.11 以前の大きな境界

`26.1` は `1.21.11` 以前の単なる次バージョンとして扱わない。mod 作成の前提がかなり変わっている。

実装時の前提:

- `1.21` から `1.21.11` は Yarn / Fabric 系の名前と API を使う
- `26.1` は Mojang mappings 系の名前と API を使う
- Java version も `1.21.x` 系は Java 21、`26.1` は Java 25
- Gradle plugin も `1.21.x` 系は `fabric-loom-remap`、`26.1` は通常の `fabric-loom`
- networking payload API も別物
- screen / key input API も別物
- villager / merchant / trade 周りの class 名・method 名も別物

具体例:

| 項目 | `1.21` - `1.21.11` | `26.1` |
| --- | --- | --- |
| mappings | Yarn | Mojang mappings |
| Java | 21 | 25 |
| Gradle | `fabric-loom-remap` | `fabric-loom` |
| player | `ServerPlayerEntity` | `ServerPlayer` |
| villager | `VillagerEntity` | `Villager` |
| merchant screen handler | `MerchantScreenHandler` | `MerchantMenu` |
| trade offer list | `TradeOfferList` | `MerchantOffers` |
| trade sync packet | `SetTradeOffersS2CPacket` | `ClientboundMerchantOffersPacket` |
| payload interface | `CustomPayload` | `CustomPacketPayload` |
| payload codec | `PacketCodec` | `StreamCodec` |
| payload buffer | `RegistryByteBuf` | `RegistryFriendlyByteBuf` |
| id class | `Identifier.of(...)` | `Identifier.fromNamespaceAndPath(...)` |
| Fabric registry | `PayloadTypeRegistry.playC2S()` / `playS2C()` | `PayloadTypeRegistry.serverboundPlay()` / `clientboundPlay()` |
| open screen/menu | `player.currentScreenHandler` | `player.containerMenu` |
| screen sync id | `merchantHandler.syncId` | `merchantMenu.containerId` |
| sneak check | `player.isSneaking()` | `player.isShiftKeyDown()` |
| key input | `KeyInput` or int key args | `KeyEvent` |

注意:

- `26.1` へ移植するときは、Yarn 名を Mojang 名に機械変換するだけでは足りない。
- `1.21.11` 以前へ `26.1` のコードをそのまま戻すのも危険。
- `1.21.9` のロック仕様を正にする場合でも、`26.1` では API 名と実装方法を 26.1 向けに作り直す。
- `26.1` の NeoForge 対応は、Fabric 26.1 を通してから NeoForge 26.1 に移す方が安全。
- `26.1` と `1.21.11` 以前は、同じ仕様を別 API で実装する別系統として扱う。

## 各バージョンでの基本手順

各ブランチで同じ流れを繰り返す。

1. 元ブランチから NeoForge ブランチを作成する。

   例:

   ```powershell
   git switch main
   git switch -c neoforge/1.21.11
   ```

2. `neoforge/26.1` のマルチローダー構成を移植する。

   方針:

   - `common/`, `fabric/`, `neoforge/` の構成を導入
   - 既存 `src/main/...` の Fabric 実装を `common` と `fabric` に分離
   - NeoForge entrypoint / `neoforge.mods.toml` / service loader を追加
   - ルート Gradle と `settings.gradle` をマルチプロジェクト向けに更新

3. `gradle.properties` を対象バージョンに合わせる。

   必ず確認する値:

   - `minecraft_version`
   - `java_version`
   - `fabric_loader_version` または既存の `loader_version`
   - `fabric_api_version`
   - `loom_version`
   - `moddevgradle_version`
   - `neoforge_version`
   - `minecraft_version_range`
   - `loader_version_range`
   - `mod_version`

   `mod_version` は全対象ブランチで `3.0.0` に揃える。

4. Yarn 名から Mojang 名への差分を確認する。

   `neoforge/26.1` では common 側が Mojang mappings 名になっている。

   特に確認する箇所:

   - `MerchantMenu`
   - `MerchantOffers`
   - `MerchantOffer`
   - `Villager`
   - `ClientboundMerchantOffersPacket`
   - `ServerPlayer`
   - `KeyEvent`
   - payload / stream codec 周辺
   - mixin target と accessor method

5. loader ごとの API 差分を修正する。

   Fabric 側:

   - `PayloadTypeRegistry`
   - `ServerPlayNetworking`
   - `ClientPlayNetworking`
   - keybind registration
   - config dir

   NeoForge 側:

   - `@Mod`
   - `IEventBus`
   - `RegisterPayloadHandlersEvent`
   - `RegisterClientPayloadHandlersEvent`
   - `RegisterKeyMappingsEvent`
   - packet distribution / client handler
   - config dir

6. リソースを loader ごとに確認する。

   Fabric:

   - `fabric/src/main/resources/fabric.mod.json`
   - `fabric/src/main/resources/META-INF/services/net.misemise.platform.PlatformHooks`

   NeoForge:

   - `neoforge/src/main/resources/META-INF/neoforge.mods.toml`
   - `neoforge/src/main/resources/META-INF/services/net.misemise.platform.PlatformHooks`

   Common:

   - mixin json
   - lang files
   - icon

7. build で確認する。

   まず compile:

   ```powershell
   .\gradlew :fabric:compileJava --console plain
   .\gradlew :neoforge:compileJava --console plain
   ```

   compile が通ったら build:

   ```powershell
   .\gradlew :fabric:build --console plain
   .\gradlew :neoforge:build --console plain
   ```

8. 動作確認する。

   最低限確認する内容:

   - Fabric client が起動する
   - NeoForge client が起動する
   - 村人取引画面でリロールキーが効く
   - スニーク必須設定が効く
   - 取引済み villager ではリロールが拒否される
   - リロール後に trade list がクライアントへ更新される
   - particle 設定が効く
   - config file が loader ごとの config dir に保存される

9. 成果物名を確認する。

   loader name + mod version + Minecraft version の順で分かる jar 名にする。

   例:

   - `reroll-trades-fabric-3.0.0-1.21.11.jar`
   - `reroll-trades-neoforge-3.0.0-1.21.11.jar`

10. commit / push する。

   例:

   ```powershell
   git add -A
   git commit -m "Port 1.21.11 branch to NeoForge"
   git push -u origin neoforge/1.21.11
   ```

## バージョン別の注意点

### `1.21.11` / `main`

- `main` は Minecraft `1.21.11`。
- `neoforge/26.1` に近いが、Minecraft バージョン表記と NeoForge の対応バージョン体系が違う可能性がある。
- `KeyEvent` や network payload 周辺は `26.1` と同じ前提で進めず、compile error を見て調整する。

### `1.21.9`

- `26.1` より少し古いが、比較的新しい API 系。
- 最初の本格的な横展開対象として良い。

### `1.21.6`

- loader / Fabric API が `1.21.9` より古い。
- client input / keybind / networking で差分が出る可能性がある。

### `1.21.5`

- ローカル `1.21.5` は現状 upstream が設定されていない。
- 作業前に `origin/1.21.5` とローカル `1.21.5` の差を確認する。
- 必要なら `origin/1.21.5` を基準にする。

確認例:

```powershell
git log --oneline 1.21.5..origin/1.21.5
git log --oneline origin/1.21.5..1.21.5
```

### `1.21.4` / `1.21.2` / `1.21`

- 古いほど mapping 名、screen handler、villager trade 更新、network payload 周りの差分が大きくなる可能性がある。
- 先に新しいバージョンで移植パターンを固めてから着手する。

## 実装時に見るべき主要ファイル

`neoforge/26.1` 側の参照元:

- `build.gradle`
- `settings.gradle`
- `gradle.properties`
- `common/src/main/java/net/misemise/RerollTrades.java`
- `common/src/main/java/net/misemise/client/RerollTradesClient.java`
- `common/src/main/java/net/misemise/config/RerollConfig.java`
- `common/src/main/java/net/misemise/network/RerollTradesPayload.java`
- `common/src/main/java/net/misemise/network/RerollParticlePayload.java`
- `common/src/main/java/net/misemise/platform/PlatformHooks.java`
- `common/src/main/java/net/misemise/platform/PlatformServices.java`
- `fabric/src/main/java/net/misemise/fabric/PlatformHooksImpl.java`
- `fabric/src/main/java/net/misemise/fabric/RerollTradesFabric.java`
- `fabric/src/main/java/net/misemise/fabric/RerollTradesFabricClient.java`
- `fabric/src/main/resources/fabric.mod.json`
- `neoforge/src/main/java/net/misemise/neoforge/PlatformHooksImpl.java`
- `neoforge/src/main/java/net/misemise/neoforge/RerollTradesNeoForge.java`
- `neoforge/src/main/java/net/misemise/neoforge/RerollTradesNeoForgeClient.java`
- `neoforge/src/main/resources/META-INF/neoforge.mods.toml`

## 先に決めたい方針

次の実装チャットで最初に決めること:

1. `common` を正式な Gradle サブプロジェクトにするか、現状通り `fabric` / `neoforge` から sourceSets で直接読むか。
2. NeoForge ブランチ名を `neoforge/1.21.11` のように Minecraft バージョン基準で統一するか。
3. jar 名を `<mod-name>-<loader-name>-<mod-version>-<minecraft-version>.jar` に揃える。
4. 全ブランチの `mod_version` を `3.0.0` に揃える。
5. 各ブランチを個別 commit / push するか、作業途中でまとめて確認するか。
6. NeoForge 側の取引済みロック保存方式を何にするか。
7. Fabric 側の loader 依存コードをどこまで整理してから NeoForge 横展開するか。

おすすめ:

- `common` は今すぐ正式サブプロジェクト化せず、`neoforge/26.1` と同じ構成を維持する。
- ブランチ名は `neoforge/<minecraft_version>` で統一する。
- jar 名は `<mod-name>-<loader-name>-<mod-version>-<minecraft-version>.jar` に揃える。
- `mod_version` は全ブランチで `3.0.0` に揃える。
- Fabric 側は「common に仕様、fabric に Fabric API」という境界を作る範囲で整理する。
- 先に `main` / `26.1` / `neoforge/26.1` の機能差を埋め、その後 1ブランチずつ compile / build して commit / push する。

## AI が間違えやすい仕様・落とし穴

実装前に必ず読むこと。ここを間違えると compile が通っても仕様退行や runtime crash になりやすい。

### 1. 現在の `neoforge/26.1` には旧 `src/main` が残っている

`neoforge/26.1` の実際の build 対象は `fabric` / `neoforge` サブプロジェクトで、共通コードはそれぞれの `sourceSets` から `../common/src/main/...` を直接読む構成。

つまり、`src/main/java/...` と `src/main/resources/...` は旧 Fabric 構成の残骸として残っている。移植時にここだけ編集しても `fabric` / `neoforge` の build には反映されない。

見るべき場所:

- 新構成: `common/`, `fabric/`, `neoforge/`
- 旧構成: `src/main/...`

注意:

- 新規 NeoForge ブランチでは、まず元ブランチの `src/main/...` を新構成へ分解する。
- 既に新構成になった後は、旧 `src/main/...` を仕様の正として編集しない。

### 2. `common/build.gradle` は現在未使用

`common/build.gradle` は存在するが、`settings.gradle` では `common` を include していない。

さらに `common/build.gradle` 内には `rootProject.architectury_api_version` が出てくるが、現在の root `gradle.properties` にはその値がない。

つまり、安易に `include("common")` を追加すると壊れる可能性が高い。現状は `fabric/build.gradle` と `neoforge/build.gradle` が `../common/src/main/java` / `../common/src/main/resources` を直接読む方式。

### 3. `fabric.mod.json` の `architectury` 依存は要確認

現在の `fabric/src/main/resources/fabric.mod.json` には:

```json
"architectury": ">=19.0.1"
```

があるが、`fabric/build.gradle` では Architectury API を依存に入れていない。また、現行 common コードも Architectury API を直接 import していない。

このままだと runtime で Fabric Loader が Architectury を要求する可能性がある。移植時に「Architectury が必要な設計なのか」「ただの残骸なのか」を確認する。

### 4. mod id / namespace が loader ごとに違う

Fabric:

- mod id: `reroll-trades`
- asset namespace: `assets/reroll-trades`
- payload namespace: `reroll-trades`

NeoForge:

- mod id: `reroll_trades`
- dependency block: `[[dependencies.reroll_trades]]`

注意:

- NeoForge の `modId` は `_`。
- assets / lang / payload は `reroll-trades` のまま。
- 「全部 `reroll_trades` に統一」や「全部 `reroll-trades` に統一」は危険。

### 5. `1.21` から `1.21.9` は取引済みロック仕様がある

`1.21`, `1.21.2`, `1.21.4`, `1.21.5`, `1.21.6`, `1.21.9` には、`RerollLockedPayload` / `RerollRejectPayload` / `VillagerEntityTradeMixin` / `VillagerLastCustomerAccessor` がある。

この系統の仕様:

- プレイヤーが村人と取引したら、その player UUID を villager の Data Attachment に保存する。
- 同じ player + villager の組み合わせでは、その後リロール不可。
- サーバー再起動後もロックが残る。
- クライアントは `RerollLockedPayload` でボタンを無効化する。
- スニーク不足や職業なしなどの一時的拒否では `RerollRejectPayload` を送り、楽観的に無効化したボタンを戻す。

`main` / `26.1` / `neoforge/26.1` にはこの永続ロック実装がない。これは今後揃えるべき機能差であり、単純に `neoforge/26.1` の common 実装で他ブランチを上書きすると、古いバージョンの最新仕様を消すことになる。

### 6. `main` と `26.1` は現状シンプルな使用済み trade 判定

`main` / `26.1` 系は、既存 offer の `uses > 0` を見てリロールを拒否するシンプルな仕様。

現状この仕様では:

- `RerollLockedPayload` はない。
- `RerollRejectPayload` はない。
- `VillagerEntityTradeMixin` もない。
- button はサーバーから永続ロックされない。

今後は古いブランチの永続ロック仕様を正として、`main` / `26.1` / `neoforge/26.1` にも同等機能を入れる。

### 7. trade 生成方法はバージョンで違う

`1.21` から `1.21.9` 系:

- `fillRecipes()` / `updateTrades()` を直接呼ばず、profession の trade table から手動で offer を再生成している。
- 理由は、既存コメント上では `fillRecipes()` の副作用で一時的な職業消失を避けるため。
- nitwit / unemployed では `message.reroll-trades.no_profession` を出し、`RerollRejectPayload` を返す。

`main`:

- `VillagerEntityAccessor#invokeFillRecipes(ServerWorld)` を使う。

`26.1` / `neoforge/26.1`:

- `VillagerEntityAccessor#rerollTrades$updateTrades(ServerLevel)` を使う。

注意:

- 古いブランチへ `neoforge/26.1` の `updateTrades` 方式をそのまま持ち込まない。
- `1.21` / `1.21.2` と `1.21.5+` では `VillagerData` と trade table の型が違う。

### 8. payload API の境目を混ぜない

`1.21` から `main`:

- `net.minecraft.network.packet.CustomPayload`
- `PacketCodec`
- `RegistryByteBuf`
- `Identifier.of(...)`
- `PayloadTypeRegistry.playC2S()` / `playS2C()`

`26.1` / `neoforge/26.1`:

- `net.minecraft.network.protocol.common.custom.CustomPacketPayload`
- `StreamCodec`
- `RegistryFriendlyByteBuf`
- `Identifier.fromNamespaceAndPath(...)`
- Fabric では `serverboundPlay()` / `clientboundPlay()`

注意:

- 名前が似ているだけで型体系が違う。
- compile error を消す目的で `ID` / `TYPE` / `CODEC` / `STREAM_CODEC` を適当に混ぜない。
- NeoForge 側の登録 API も `PayloadRegistrar` なので Fabric の登録と同じ形にはしない。

### 9. key input API はバージョンで違う

`1.21` から `1.21.6`:

- `keyPressed(int keyCode, int scanCode, int modifiers)`
- `rerollKey.matchesKey(keyCode, scanCode)`

`1.21.9` / `main`:

- `keyPressed(KeyInput keyInput)`
- `rerollKey.matchesKey(keyInput)`

`26.1` / `neoforge/26.1`:

- `keyPressed(KeyEvent event)`
- Fabric: `KeyMapping.matches(event)`
- NeoForge: `REROLL_KEY.isActiveAndMatches(InputConstants.getKey(event))`

注意:

- `@Override` できるバージョンと `@Inject(method = "keyPressed", ...)` で処理すべきバージョンが違う。
- `require = 0` が付いている古いブランチは、対象 method の所在差を吸収するためなので消さない。
- NeoForge は GUI 上でキーを使うため `KeyConflictContext.GUI` が重要。

### 10. `IRerollLockable` は mixin package に置かない

古いロック仕様系では `IRerollLockable` を通じて `MerchantScreenMixin` に lock / unlock を呼ぶ。

重要:

- `IRerollLockable` は `net.misemise` package に置く。
- `net.misemise.mixin` package には置かない。
- `client.currentScreen instanceof MerchantScreenMixin` は runtime で成り立たないので使わない。
- `client.currentScreen instanceof IRerollLockable` を使う。

`1.21.5` はローカルブランチと `origin/1.21.5` に差がある。`origin/1.21.5` は BOM や import 欠落に見える差分があり、作業前に `1.21.6` と照合して正しい形を決める。

### 11. client-only 型を common/server 側へ漏らさない

現在の `neoforge/26.1` では `PlatformHooks` / `PlatformServices` が `net.minecraft.client.input.KeyEvent` を型として持っている。

一方で `RerollConfig` や `RerollTrades` の server-side 処理から `PlatformServices` が参照される。

これは dedicated server で client-only class を解決して落ちるリスクがある。今後の移植では、key 判定を client 専用 service に分ける、または common/server から client-only 型を参照しない形を検討する。

### 12. ServiceLoader は service file が欠けると即死する

`PlatformServices` は `ServiceLoader.load(PlatformHooks.class).findFirst().orElseThrow(...)` で実装を取る。

必須ファイル:

- `fabric/src/main/resources/META-INF/services/net.misemise.platform.PlatformHooks`
- `neoforge/src/main/resources/META-INF/services/net.misemise.platform.PlatformHooks`

注意:

- service file の中身は実装クラスの完全修飾名だけ。
- Fabric / NeoForge の service file を入れ忘れると config 読み込み時などに落ちる。
- loader ごとの resources に置く。common に片方だけ置かない。

### 13. mixin target / accessor 名は mapping に引きずられる

`26.1` / `neoforge/26.1`:

- `MerchantMenu` の field accessor は `@Accessor("trader")`
- `AbstractVillager#updateTrades(ServerLevel)` を invoker

`main`:

- `MerchantScreenHandler` の field accessor は `@Accessor("merchant")`
- `MerchantEntity#fillRecipes(ServerWorld)` を invoker

`1.21` から `1.21.9`:

- `MerchantScreenHandler` の field accessor は `@Accessor("merchant")`
- ロック仕様では `VillagerLastCustomerAccessor` と `VillagerEntityTradeMixin` が必要

注意:

- Yarn 名と Mojang 名を混ぜない。
- mixin json に登録する class もブランチごとに違う。

### 14. build 構成は branch ごとに違う

`1.21` から `main`:

- `net.fabricmc.fabric-loom-remap`
- Java 21
- Yarn mappings 明示
- single-project Fabric build

`26.1`:

- `net.fabricmc.fabric-loom`
- Java 25
- single-project Fabric build

`neoforge/26.1`:

- root で `fabric-loom` と `net.neoforged.moddev` を apply false
- `fabric` / `neoforge` サブプロジェクト
- Java 25

注意:

- 古いブランチに Java 25 を入れない。
- `fabric-loom-remap` と通常 `fabric-loom` の差を無視しない。
- `yarn_mappings` が必要なブランチと不要なブランチがある。
- `26.1` は `1.21.11` 以前と mod 作成の前提が違うため、同じテンプレートで一括処理しない。

### 15. 動作確認は build だけでは足りない

最低限、以下を loader ごとに見る。

- client が起動する
- dedicated server で client-only class crash がない
- 村人画面にボタンが出る
- GUI 内で R key が効く
- `requireSneaking` true で拒否される
- 拒否後にボタンが戻る、または対象仕様通りロックされたままになる
- 職業なし / nitwit の挙動
- 取引後のロック挙動
- リロール後に画面を閉じずに trade list が更新される
- particle packet が client に届く
- config が loader ごとの config dir に作成される

## 次チャットでの最初の依頼文案

```text
PLAN.md に沿って、まず実装前に全ブランチの機能差を揃えるところから始めて。
正とする仕様は、1.21〜1.21.9 系にある取引済み永続ロック仕様。
最初に Fabric 側コードを common と fabric 専用部分に整理しつつ、main、26.1、neoforge/26.1 に同等のロック仕様を入れて、mod_version も全対象ブランチで揃える方針にして。
mod_version は全ブランチで 3.0.0 にして。
このチャットの方針通り、コード変更後は各 loader の compile と build、可能なら client/dedicated server 起動まで確認して。
```

## 完了条件

全対象ブランチについて以下が満たされたら完了。

- `neoforge/<version>` ブランチが存在する
- 全対象ブランチの `mod_version` が `3.0.0` になっている
- 全対象ブランチで取引済み永続ロック仕様が入っている
- Fabric build が通る
- NeoForge build が通る
- 生成 jar 名が `<mod-name>-<loader-name>-<mod-version>-<minecraft-version>.jar` になっている
- 最低限の client 起動と villager trade reroll 動作が確認済み
- 各ブランチが origin に push 済み

## 追加対応: `1.21.5` から `1.21.11` の間の NeoForge 正式版

2026-06-14 に公式 NeoForge Maven metadata を確認した結果、当初作成した NeoForge ブランチには `1.21.5` と `1.21.11` の間に未対応の正式版レンジがある。

現在の NeoForge 対応済みブランチ:

- `neoforge/1.21-1.21.1`
- `neoforge/1.21.4`
- `neoforge/1.21.5`
- `neoforge/1.21.11`
- `neoforge/26.1`

公式 NeoForge release status:

- `21.6`: beta only。正式版のみ対応方針のため単独ブランチは作らない。
- `21.7`: beta only。正式版のみ対応方針のため単独ブランチは作らない。
- `21.8`: stable release あり。最新確認版は `21.8.53`。
- `21.9`: beta only。正式版のみ対応方針のため単独ブランチは作らない。
- `21.10`: stable release あり。最新確認版は `21.10.64`。
- `21.11`: stable release あり。既存 `neoforge/1.21.11` は `21.11.42` を使用中。

追加で作成する NeoForge ブランチ:

1. `neoforge/1.21.6-1.21.8`
   - base: 既存 `1.21.6` Fabric ブランチを第一候補にする。
   - `minecraft_version=1.21.8`
   - `neoforge_version=21.8.53`
   - `java_version=21`
   - `minecraft_version_range=[1.21.6,1.21.9)` は、実際に同レンジで動作確認できた場合のみ採用する。
   - レンジ互換が確認できない場合は `[1.21.8,1.21.9)` に狭める。

2. `neoforge/1.21.9-1.21.10`
   - base: 既存 `1.21.9` Fabric ブランチを第一候補にする。
   - `minecraft_version=1.21.10`
   - `neoforge_version=21.10.64`
   - `java_version=21`
   - `minecraft_version_range=[1.21.9,1.21.11)` は、実際に同レンジで動作確認できた場合のみ採用する。
   - レンジ互換が確認できない場合は `[1.21.10,1.21.11)` に狭める。

この追加対応でも維持する方針:

- snapshot version は対応しない。
- beta only の NeoForge version は正式対応ブランチにしない。
- `mod_version=3.0.0` を維持する。
- jar 名は `<mod-name>-<loader-name>-<mod-version>-<minecraft-version>.jar` の形式を維持する。
- リロールは実際に取引するまで何度でも可能にする。
- 取引後のみ、同じ player + villager の組み合わせでリロールをロックする。

追加ブランチごとの確認:

- `.\gradlew :neoforge:build --console plain`
- `.\gradlew :neoforge:runClient --console plain`
- 取引前に複数回リロールできること
- 取引後にリロールがロックされること
- ロック済み villager では server から client へ locked state が返ること
