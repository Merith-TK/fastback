/*
 * FastBack - Fast, incremental Minecraft backups powered by Git.
 * Copyright (C) 2022 pcal.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package net.pcal.fastback.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.logging.UserMessage;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


import static net.pcal.fastback.commands.Commands.*;

/**
 * @author merith-tk
 * @since dev-april-19-2024
 */
enum DownloadGitCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "install-local-git";
//    private static final String ARGUMENT = "enable";

    private static final String GITHUB_API_URL = "https://api.github.com/repos/git-for-windows/git/releases/latest";
    private static final String DOWNLOAD_DIR = ".fastback/dl";

    @Override
    public void register(LiteralArgumentBuilder<CommandSourceStack> argb, PermissionsFactory<CommandSourceStack> pf) {
        argb.then(net.minecraft.commands.Commands.literal(COMMAND_NAME)
                .requires(subcommandPermission(COMMAND_NAME, pf))
                .executes(DownloadGitCommand::execute)
        );
    }

    private static int execute(CommandContext<CommandSourceStack> cc) {
        final UserLogger log = UserLogger.ulog(cc);

        // if os is windows/64, or linux/64, or mac/64
        if (System.getProperty("os.name").toLowerCase().contains("windows") && System.getProperty("os.arch").contains("64")) {
            log.message(UserMessage.raw("Downloading latest Git for Windows release..."));
            // remove the old download directory
            try {
                Files.delete(Paths.get(DOWNLOAD_DIR));
            } catch (IOException e) {
                // ignore
            }
            CompletableFuture.runAsync(() -> {
                try {
                    URL url = new URL(GITHUB_API_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JsonParser parser = new JsonParser();
                    JsonObject release = parser.parse(response.toString()).getAsJsonObject();
                    JsonArray assets = release.getAsJsonArray("assets");

                    for (int i = 0; i < assets.size(); i++) {
                        JsonObject asset = assets.get(i).getAsJsonObject();
                        String name = asset.get("name").getAsString();
                        if (name.matches( "MinGit-.*-64-bit\\.zip")) {
                            String downloadUrl = asset.get("browser_download_url").getAsString();
                            String destinationPath = Paths.get(DOWNLOAD_DIR, name).toString();
                            File file = new File(destinationPath);
                            if (!file.exists()) {
                                downloadFile(downloadUrl, destinationPath);
                            } else {
                                log.message(UserMessage.raw("File " + destinationPath + " already exists. Skipping download."));
                            }
                            break;
                        }
                    }
                    log.message(UserMessage.raw("Downloaded latest Git for Windows release."));
                    log.message(UserMessage.raw("Extracting to " + Paths.get(".fastback/git").toAbsolutePath().toString()));
                    // extract the zipfile to ./.fastback/git
                    unzip(Paths.get(DOWNLOAD_DIR, "MinGit-*.zip").toString(), Paths.get(".fastback/git").toAbsolutePath().toString());
                    log.message(UserMessage.raw("Extraction complete."));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to get latest release: " + e.getMessage());
                }
        });
        } else {
            log.message(UserMessage.raw("This command is only available on Windows 64-bit."));
            return FAILURE;
        }


        return SUCCESS;
    }

    private static void downloadFile(String url, String destination) throws Exception {
        InputStream in = new URL(url).openStream();
        // ensure destination path exists
        Files.createDirectories(Paths.get(destination).getParent());

        Files.copy(in, Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);
    }
    private static void unzip(String zipFilePath, String destDir) {
        File dir = new File(destDir);
        // create output directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(destDir));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        byte[] buffer = new byte[1024];
        try (FileInputStream fis = new FileInputStream(zipFilePath);
             ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                File newFile = new File(destDir + File.separator + fileName);
                System.out.println("Unzipping to " + newFile.getAbsolutePath());
                //create directories for sub directories in zip
                new File(newFile.getParent()).mkdirs();
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                //close this ZipEntry
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            //close last ZipEntry
            zis.closeEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
