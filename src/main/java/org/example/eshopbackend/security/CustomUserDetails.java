package org.example.eshopbackend.security;

import lombok.AllArgsConstructor;
import org.example.eshopbackend.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/*
Třída CustomUserDetails slouží jako adaptér mezi tvou vlastní entitou User a rozhraním Spring Security UserDetails.

    Uchování entity
    Konstruktor přijímá instanci třídy User a ukládá ji do privátní proměnné, aby na ni mohl kdykoli odkazovat.

    Autority (role)
    Metoda getAuthorities() vezme enum role z objektu User a překlopí ho na seznam (Collection) instancí SimpleGrantedAuthority, které Spring Security využívá k rozhodování o přístupu k jednotlivým URL nebo metodám.

    Přihlašovací údaje

        getUsername() vrací uživatelské jméno (userName), pod kterým se uživatel přihlašuje.

        getPassword() vrací hash hesla (password), který Spring Security porovnává s tím, co uživatel zadá.

    Stav účtu
    Metody isAccountNonExpired(), isAccountNonLocked(), isCredentialsNonExpired() a isEnabled() kontrolují, zda je účet stále aktivní, není zablokovaný, zda heslo nevypršelo a zda uživatel potvrdil registraci. V základní implementaci vracíme vždy true, ale můžeme je upravit podle vlastních sloupců v tabulce (např. boolean enabled).

    Další data
    Kromě povinných metod UserDetails přidáváme i pomocné gettery (getEmail(), getPhoneNumber(), getId()), abychom v kontrolérech či službách mohli pohodlně získat i další informace o právě přihlášeném uživateli.

Díky tomu Spring Security ví, jaké role má uživatel, jaké má přihlašovací údaje a zda je jeho účet v pořádku, aniž by framework musel znát detaily tvé doménové entity User.
 */

@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE" + user.getRole().name())
        );
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public String getEmail(){
        return user.getEmail();
    }
    public String getPhoneNumber(){
        return user.getPhoneNumber();
    }
    public Long getUserId(){
        return user.getUserID();
    }

    public String getFirstName(){
        return user.getFirstName();
    }
    public String getLastName(){
        return user.getLastName();
    }
}
