package ru.students.config;

import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

@Configuration
public class WebSecurityConf {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests((authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()

                        .requestMatchers("/").permitAll()
                        .requestMatchers("/login/**").permitAll()
                        .requestMatchers("/register/**").permitAll()
                        .requestMatchers("/images/**").permitAll()
                        .requestMatchers("/css/**").permitAll()
                        .requestMatchers("/forum").permitAll()
                        .requestMatchers("/getPeers").permitAll()
                        .anyRequest().authenticated())).formLogin(form ->
                        form.loginPage("/login")
                                .loginProcessingUrl("/login")
                                .defaultSuccessUrl("/")
                                .permitAll())
                .logout(logout -> logout.logoutUrl("logout").permitAll()).csrf().disable();
        return httpSecurity.build();
    }

    @Bean
    public Tracker createTracker() throws IOException {
        Tracker tracker = new Tracker(6969);
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".torrent");
            }
        };

        for (File f : new File("C:\\Users\\MasterIlidan\\IdeaProjects\\ttorrent\\staging").listFiles(filter)) {
            tracker.announce(TrackedTorrent.load(f));
        }

//Also you can enable accepting foreign torrents.
//if tracker accepts request for unknown torrent it starts tracking the torrent automatically
        tracker.setAcceptForeignTorrents(false);

// Once done, you just have to start the tracker's main operation loop:
        tracker.start(true);
        return tracker;
    }

}
